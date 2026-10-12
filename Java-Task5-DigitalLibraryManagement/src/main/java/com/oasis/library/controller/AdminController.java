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
import java.util.stream.Collectors;

import static com.oasis.library.controller.UserController.back;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final List<Reservation.Status> ACTIVE = List.of(Reservation.Status.WAITING, Reservation.Status.READY);

    private final BookRepository books;
    private final UserRepository users;
    private final IssueRepository issues;
    private final ReservationRepository reservations;
    private final ContactMessageRepository messages;
    private final LibraryService library;

    public AdminController(BookRepository books, UserRepository users, IssueRepository issues,
                           ReservationRepository reservations, ContactMessageRepository messages,
                           LibraryService library) {
        this.books = books;
        this.users = users;
        this.issues = issues;
        this.reservations = reservations;
        this.messages = messages;
        this.library = library;
    }

    // ---------------------------------------------------------------- dashboard

    @GetMapping
    public String dashboard(Model model) {
        library.expireHolds();
        List<Issue> open = issues.findByReturnDateIsNullOrderByDueDateAsc();
        List<Issue> overdue = open.stream().filter(Issue::isOverdue).toList();
        long pendingFines = issues.findWithFines().stream().filter(i -> !i.isFinePaid()).mapToLong(Issue::getFine).sum()
                + overdue.stream().mapToLong(i -> i.currentFine(library.getFinePerDay())).sum();

        // books per category for the chart
        Map<String, Integer> perCategory = books.findAll().stream()
                .collect(Collectors.groupingBy(Book::getCategory, TreeMap::new, Collectors.summingInt(Book::getQuantity)));
        int maxCat = perCategory.values().stream().max(Integer::compare).orElse(1);

        model.addAttribute("bookCount", books.count());
        model.addAttribute("copyCount", books.totalCopies());
        model.addAttribute("memberCount", users.countByRole(User.Role.USER));
        model.addAttribute("issuedCount", open.size());
        model.addAttribute("overdueCount", overdue.size());
        model.addAttribute("pendingFines", pendingFines);
        model.addAttribute("collectedFines", issues.collectedFines());
        model.addAttribute("bookingCount", reservations.countByStatusIn(ACTIVE));
        model.addAttribute("overdue", overdue);
        model.addAttribute("recent", issues.findAllByOrderByIssueDateDesc().stream().limit(6).toList());
        model.addAttribute("perCategory", perCategory);
        model.addAttribute("maxCat", maxCat);
        model.addAttribute("perDay", library.getFinePerDay());
        return "admin/dashboard";
    }

    // ---------------------------------------------------------------- books

    @GetMapping("/books")
    public String books(@RequestParam(required = false) String q, @RequestParam(required = false) String category, Model model) {
        model.addAttribute("books", books.search(StringUtils.hasText(category) ? category : null,
                StringUtils.hasText(q) ? q.trim() : null));
        model.addAttribute("categories", books.findCategories());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        return "admin/books";
    }

    @GetMapping("/books/new")
    public String newBook(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", books.findCategories());
        return "admin/book-form";
    }

    @GetMapping("/books/{id}/edit")
    public String editBook(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Book> b = books.findById(id);
        if (b.isEmpty()) {
            ra.addFlashAttribute("error", "Book not found.");
            return "redirect:/admin/books";
        }
        model.addAttribute("book", b.get());
        model.addAttribute("categories", books.findCategories());
        return "admin/book-form";
    }

    @PostMapping("/books/save")
    public String saveBook(@Valid @ModelAttribute("book") Book book, BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                Book saved = library.saveBook(book);
                ra.addFlashAttribute("success", "\"" + saved.getTitle() + "\" saved.");
                return "redirect:/admin/books";
            } catch (LibraryException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("categories", books.findCategories());
        return "admin/book-form";
    }

    @PostMapping("/books/{id}/delete")
    public String deleteBook(@PathVariable Long id, RedirectAttributes ra) {
        try {
            library.deleteBook(id);
            ra.addFlashAttribute("success", "Book deleted.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/books";
    }

    // ---------------------------------------------------------------- issues

    @GetMapping("/issues")
    public String issues(@RequestParam(defaultValue = "active") String filter, Model model) {
        List<Issue> list = switch (filter) {
            case "overdue" -> issues.findByReturnDateIsNullOrderByDueDateAsc().stream().filter(Issue::isOverdue).toList();
            case "all" -> issues.findAllByOrderByIssueDateDesc();
            default -> issues.findByReturnDateIsNullOrderByDueDateAsc();
        };
        model.addAttribute("issues", list);
        model.addAttribute("filter", filter);
        model.addAttribute("perDay", library.getFinePerDay());
        model.addAttribute("bookings", reservations.findByStatusInOrderByCreatedAtAsc(ACTIVE));
        return "admin/issues";
    }

    @PostMapping("/issues/{id}/return")
    public String adminReturn(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra) {
        try {
            Issue i = library.returnBook(me, id);
            ra.addFlashAttribute("success", "Return processed for \"" + i.getBook().getTitle() + "\""
                    + (i.getFine() > 0 ? " — fine ₹" + i.getFine() : "") + ".");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/issues";
    }

    // ---------------------------------------------------------------- members

    @GetMapping("/members")
    public String members(Model model) {
        List<User> list = users.findAllByOrderByCreatedAtDesc();
        Map<Long, Long> loans = new HashMap<>();
        Map<Long, Long> fines = new HashMap<>();
        for (User u : list) {
            loans.put(u.getId(), issues.countByUserAndReturnDateIsNull(u));
            fines.put(u.getId(), library.outstandingFines(u));
        }
        model.addAttribute("members", list);
        model.addAttribute("loans", loans);
        model.addAttribute("fines", fines);
        return "admin/members";
    }

    @GetMapping("/members/{id}")
    public String member(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> u = users.findById(id);
        if (u.isEmpty()) {
            ra.addFlashAttribute("error", "Member not found.");
            return "redirect:/admin/members";
        }
        model.addAttribute("member", u.get());
        model.addAttribute("memberIssues", issues.findByUserOrderByIssueDateDesc(u.get()));
        model.addAttribute("memberBookings", reservations.findByUserOrderByCreatedAtDesc(u.get()));
        model.addAttribute("outstanding", library.outstandingFines(u.get()));
        model.addAttribute("perDay", library.getFinePerDay());
        return "admin/member-detail";
    }

    @PostMapping("/members/{id}/toggle")
    public String toggle(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra,
                         @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            library.toggleMember(id, me);
            ra.addFlashAttribute("success", "Member status updated.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/admin/members");
    }

    @PostMapping("/members/{id}/delete")
    public String deleteMember(@PathVariable Long id, @ModelAttribute("me") User me, RedirectAttributes ra) {
        try {
            library.deleteMember(id, me);
            ra.addFlashAttribute("success", "Member removed.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/members";
    }

    // ---------------------------------------------------------------- fines

    @GetMapping("/fines")
    public String fines(Model model) {
        List<Issue> settled = issues.findWithFines();
        List<Issue> accruing = issues.findByReturnDateIsNullOrderByDueDateAsc().stream().filter(Issue::isOverdue).toList();
        List<Issue> unpaidSettled = settled.stream().filter(i -> !i.isFinePaid()).toList();
        List<Issue> paidSettled = settled.stream().filter(Issue::isFinePaid).toList();
        model.addAttribute("settled", settled);
        model.addAttribute("unpaidSettled", unpaidSettled);
        model.addAttribute("paidSettled", paidSettled);
        model.addAttribute("accruing", accruing);
        model.addAttribute("perDay", library.getFinePerDay());
        model.addAttribute("unpaidTotal", settled.stream().filter(i -> !i.isFinePaid()).mapToLong(Issue::getFine).sum());
        model.addAttribute("paidTotal", issues.collectedFines());
        model.addAttribute("accruingTotal", accruing.stream().mapToLong(i -> i.currentFine(library.getFinePerDay())).sum());
        return "admin/fines";
    }

    @PostMapping("/fines/{id}/pay")
    public String pay(@PathVariable Long id, RedirectAttributes ra,
                      @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            library.markFinePaid(id);
            ra.addFlashAttribute("success", "Fine marked as paid.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/admin/fines");
    }

    @PostMapping("/fines/{id}/waive")
    public String waive(@PathVariable Long id, RedirectAttributes ra,
                        @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            library.waiveFine(id);
            ra.addFlashAttribute("success", "Fine waived.");
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(referer, "/admin/fines");
    }

    // ---------------------------------------------------------------- messages

    @GetMapping("/messages")
    public String messages(Model model) {
        model.addAttribute("messages", messages.findAllByOrderByResolvedAscCreatedAtDesc());
        return "admin/messages";
    }

    @PostMapping("/messages/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam(required = false) String reply, RedirectAttributes ra) {
        messages.findById(id).ifPresentOrElse(m -> {
            if (StringUtils.hasText(reply)) m.setAdminReply(reply.trim());
            m.setResolved(true);
            messages.save(m);
            ra.addFlashAttribute("success", "Query marked as resolved.");
        }, () -> ra.addFlashAttribute("error", "Message not found."));
        return "redirect:/admin/messages";
    }

    @PostMapping("/messages/{id}/delete")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes ra) {
        messages.deleteById(id);
        ra.addFlashAttribute("success", "Message deleted.");
        return "redirect:/admin/messages";
    }
}
