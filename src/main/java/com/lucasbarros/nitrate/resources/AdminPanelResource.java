package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.AdminBadgeAwardDTO;
import com.lucasbarros.nitrate.dto.AdminStatsDTO;
import com.lucasbarros.nitrate.dto.BadgeAwardDTO;
import com.lucasbarros.nitrate.dto.BadgeDTO;
import com.lucasbarros.nitrate.services.AdminService;
import com.lucasbarros.nitrate.services.BadgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** painel de admin no site; o acesso (contas de ADMIN_EMAILS) é conferido no SecurityConfig */
@RestController
@RequestMapping("/admin")
public class AdminPanelResource {

    @Autowired
    private AdminService adminService;

    @Autowired
    private BadgeService badgeService;

    @GetMapping("/stats")
    public AdminStatsDTO stats() {
        return adminService.stats();
    }

    // todos os selos que existem, para o admin escolher
    @GetMapping("/badges/catalog")
    public List<BadgeDTO> catalogo() {
        return badgeService.catalogo();
    }

    // selos já concedidos, do mais recente para o mais antigo
    @GetMapping("/badges")
    public List<AdminBadgeAwardDTO> concedidos() {
        return badgeService.todos();
    }

    @GetMapping("/users/{username}/badges")
    public List<BadgeDTO> selosDoUsuario(@PathVariable String username) {
        return badgeService.doUsuario(username);
    }

    // concede (ou atualiza a nota de) um selo; corpo opcional: {"note": "..."}
    @PutMapping("/users/{username}/badges/{type}")
    public BadgeDTO conceder(@PathVariable String username, @PathVariable String type,
                             @RequestBody(required = false) BadgeAwardDTO request) {
        return badgeService.conceder(username, type, request == null ? null : request.note());
    }

    @DeleteMapping("/users/{username}/badges/{type}")
    public ResponseEntity<Void> remover(@PathVariable String username, @PathVariable String type) {
        badgeService.remover(username, type);
        return ResponseEntity.noContent().build();
    }
}
