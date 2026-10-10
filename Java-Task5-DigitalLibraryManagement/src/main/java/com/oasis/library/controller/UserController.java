package com.oasis.library.controller;

import com.oasis.library.model.*;
import com.oasis.library.repository.*;
import com.oasis.library.service.LibraryException;
import com.oasis.library.service.LibraryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/** Member-facing pages: catalogue, dashboard, issue/return/reserve and contact form. */
@Controller
public class UserController {

    private static final List<Reservation.Status> ACTIVE = List.of(Reservation.Status.WAITING, Reservation.Status.READY);

    private final BookRepository books;
    private final IssueRepository issues;
    private final ReservationRepository reservations;
    private final ContactMessageRepository messages;
    private final LibraryService library;

    public UserController(BookRepository books, IssueRepository issues, ReservationRepository reservations,
                          ContactMessageRepository messages, LibraryService library) {
        this.books = books;
        this.issues = issues;
        this.reservations = reservations;
        this.messages = messages;
        this.library = library;
    }

    // ---------------------------------------------------------------- catalogue

    @GetMapping("/books")
    public String catalogue(@RequestParam(required = false) String category,
                            @RequestParam(required = false) String q,
                            @ModelAttribute("me") User me, Model model) {
        library.expireHolds();
        String cat = StringUtils.hasText(category) ? category : null;
        String query = StringUtils.hasText(q) ? q.trim() : null;
        List<Book> list = books.search(cat, query);

        Set<Long> myLoans = new HashSet<>();
        issues.findByUserOrderByIssueDateDesc(me).stream().filter(i -> !i.isReturned())
                .forEach(i -> myLoans.add(i.getBook().getId()));
        Set<Long> myBookings = new HashSet<>();
        reservations.findByUserOrderByCreatedAtDesc(me).stream().filter(Reservation::isActive)
                .forEach(r -> myBookings.add(r.getBook().getId()));
        Map<Long, Long> shelf = new HashMap<>();   // copies that can be issued right now
        for (Book b : list) {
            long held = reservations.countByBookAndStatus(b, Reservation.Status.READY);
            shelf.put(b.getId(), Math.max(0, b.getAvailable() - held));
        }

        model.addAttribute("books", list);
        model.addAttribute("categories", books.findCategories());
        model.addAttribute("category", cat);
        model.addAttribute("q", query);
        model.addAttribute("myLoans", myLoans);
        model.addAttribute("myBookings", myBookings);
        model.addAttribute("shelf", shelf);
        return "user/books";
    }

    @PostMapping("/books/{id}/issue")
    public String issue(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            Issue i = library.issueBook(me, id);
            ra.addFlashAttribute("success", "\"" + i.getBook().getTitle() + "\" issued! Due on " + i.getDueDate() + ".");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/books");
    }

    @PostMapping("/books/{id}/reserve")
    public String reserve(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra,
                          @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            Reservation r = library.reserve(me, id);
            ra.addFlashAttribute("success", "Advance booking placed for \"" + r.getBook().getTitle()
                    + "\". You are #" + library.queuePosition(r) + " in the queue.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/books");
    }

    // ---------------------------------------------------------------- dashboard

    @GetMapping("/dashboard")
    public String dashboard(@ModelAttribute("me") User me, Model model) {
        if (me.isAdmin()) return "redirect:/admin";
        library.expireHolds();
        List<Issue> all = issues.findByUserOrderByIssueDateDesc(me);
        List<Issue> active = all.stream().filter(i -> !i.isReturned()).toList();
        List<Issue> history = all.stream().filter(Issue::isReturned).toList();
        List<Reservation> bookings = reservations.findByUserOrderByCreatedAtDesc(me);
        Map<Long, Integer> positions = new HashMap<>();
        bookings.forEach(r -> positions.put(r.getId(), library.queuePosition(r)));

        model.addAttribute("active", active);
        model.addAttribute("history", history);
        model.addAttribute("bookings", bookings);
        model.addAttribute("positions", positions);
        model.addAttribute("outstanding", library.outstandingFines(me));
        model.addAttribute("overdueCount", active.stream().filter(Issue::isOverdue).count());
        model.addAttribute("readyCount", bookings.stream().filter(r -> r.getStatus() == Reservation.Status.READY).count());
        model.addAttribute("perDay", library.getFinePerDay());
        model.addAttribute("maxLoans", library.getMaxActiveLoans());
        model.addAttribute("myMessages", messages.findByEmailIgnoreCaseOrderByCreatedAtDesc(me.getEmail()));
        return "user/dashboard";
    }

    @PostMapping("/issues/{id}/return")
    public String returnBook(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra,
                             @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            Issue i = library.returnBook(me, id);
            String msg = "\"" + i.getBook().getTitle() + "\" returned.";
            if (i.getFine() > 0) msg += " Overdue by " + i.getDaysOverdue() + " day(s) — fine of ₹" + i.getFine() + " generated.";
            ra.addFlashAttribute(i.getFine() > 0 ? "warning" : "success", msg);
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/dashboard");
    }

    @PostMapping("/issues/{id}/renew")
    public String renew(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            library.renew(me, id);
            ra.addFlashAttribute("success", "Loan renewed for 7 more days.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/dashboard");
    }

    @PostMapping("/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra,
                         @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            library.cancelReservation(me, id);
            ra.addFlashAttribute("success", "Booking cancelled.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/dashboard");
    }

    // ---------------------------------------------------------------- contact

    @GetMapping("/contact")
    public String contactForm(Model model) {
        User me = (User) model.getAttribute("me");   // null for anonymous visitors
        ContactMessage m = new ContactMessage();
        if (me != null) { m.setName(me.getFullName()); m.setEmail(me.getEmail()); }
        model.addAttribute("msg", m);
        return "contact";
    }

    @PostMapping("/contact")
    public String contact(@Valid @ModelAttribute("msg") ContactMessage msg, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return "contact";
        messages.save(msg);
        ra.addFlashAttribute("success", "Thanks " + msg.getName() + "! Your query has been sent to the librarian.");
        return "redirect:/contact";
    }

    /** Redirect back only to local pages (avoids open-redirects via the Referer header). */
    static String back(String referer, String fallback) {
        if (referer != null) {
            try {
                java.net.URI u = java.net.URI.create(referer);
                String path = u.getRawPath() + (u.getRawQuery() != null ? "?" + u.getRawQuery() : "");
                if (path.startsWith("/") && !path.startsWith("//")) return "redirect:" + path;
            } catch (IllegalArgumentException ignored) { }
        }
        return "redirect:" + fallback;
    }
}
