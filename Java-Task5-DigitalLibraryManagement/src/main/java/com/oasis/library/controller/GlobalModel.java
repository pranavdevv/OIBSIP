package com.oasis.library.controller;

import com.oasis.library.model.User;
import com.oasis.library.repository.ContactMessageRepository;
import com.oasis.library.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Exposes the logged-in user (and admin badge counts) to every template. */
@ControllerAdvice
public class GlobalModel {

    private final UserRepository users;
    private final ContactMessageRepository messages;

    public GlobalModel(UserRepository users, ContactMessageRepository messages) {
        this.users = users;
        this.messages = messages;
    }

    @ModelAttribute("me")
    public User me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return null;
        return users.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    @ModelAttribute("openMessages")
    public long openMessages(Authentication auth) {
        boolean admin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? messages.countByResolvedFalse() : 0;
    }
}
