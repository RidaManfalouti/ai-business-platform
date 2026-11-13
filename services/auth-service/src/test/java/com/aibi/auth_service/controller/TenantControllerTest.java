package com.aibi.auth_service.controller;

import com.aibi.auth_service.entity.Membership;
import com.aibi.auth_service.entity.Tenant;
import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.MembershipRepository;
import com.aibi.auth_service.repository.TenantRepository;
import com.aibi.auth_service.repository.UserRepository;
import com.aibi.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser1;
    private User testUser2;
    private String tokenUser1;
    private String tokenUser2;

    @BeforeEach
    void setUp() {
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        testUser1 = userRepository.save(User.builder()
                .username("tuser1_" + suffix1)
                .email("tuser1_" + suffix1 + "@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(User.Role.USER)
                .build());
        tokenUser1 = jwtService.generateToken(testUser1);

        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        testUser2 = userRepository.save(User.builder()
                .username("tuser2_" + suffix2)
                .email("tuser2_" + suffix2 + "@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(User.Role.USER)
                .build());
        tokenUser2 = jwtService.generateToken(testUser2);
    }

    @Test
    @DisplayName("Création d'entreprise : Le créateur devient OWNER et reçoit un JWT avec le tenant_id")
    void shouldCreateTenantAndAssignOwner() throws Exception {
        String testSlug = "acme-innovations-" + UUID.randomUUID().toString().substring(0, 6);
        String payload = """
                {
                    "name": "Acme Innovations",
                    "slug": "%s"
                }
                """.formatted(testSlug);

        var result = mockMvc.perform(post("/api/auth/tenants")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId", notNullValue()))
                .andExpect(jsonPath("$.tenantSlug").value(testSlug))
                .andExpect(jsonPath("$.tenantRole").value("OWNER"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertTrue(responseJson.contains("Acme Innovations"));

        Tenant tenant = tenantRepository.findBySlug(testSlug).orElseThrow();
        assertEquals(Tenant.TenantStatus.ACTIVE, tenant.getStatus());

        Membership membership = membershipRepository.findByTenantIdAndUserId(tenant.getId(), testUser1.getId()).orElseThrow();
        assertEquals(Membership.TenantRole.OWNER, membership.getRole());
        assertEquals(Membership.MembershipStatus.ACTIVE, membership.getStatus());
    }

    @Test
    @DisplayName("Changement d'entreprise (Switch) : Refus HTTP 403 si l'utilisateur n'est pas membre")
    void shouldRejectSwitchIfUserIsNotMember() throws Exception {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name("Foreign Corp")
                .slug("foreign-corp-" + UUID.randomUUID().toString().substring(0, 6))
                .status(Tenant.TenantStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/auth/tenants/" + tenant.getId() + "/switch")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Changement d'entreprise (Switch) : Refus HTTP 403 si l'adhésion est suspendue")
    void shouldRejectSwitchIfMembershipIsSuspended() throws Exception {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name("Suspended Member Corp")
                .slug("susp-mem-" + UUID.randomUUID().toString().substring(0, 6))
                .status(Tenant.TenantStatus.ACTIVE)
                .build());

        membershipRepository.save(Membership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant.getId())
                .userId(testUser1.getId())
                .role(Membership.TenantRole.MANAGER)
                .status(Membership.MembershipStatus.SUSPENDED)
                .build());

        mockMvc.perform(post("/api/auth/tenants/" + tenant.getId() + "/switch")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Changement d'entreprise (Switch) : Refus HTTP 403 si l'entreprise est suspendue")
    void shouldRejectSwitchIfTenantIsSuspended() throws Exception {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name("Suspended Company")
                .slug("susp-comp-" + UUID.randomUUID().toString().substring(0, 6))
                .status(Tenant.TenantStatus.SUSPENDED)
                .build());

        membershipRepository.save(Membership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant.getId())
                .userId(testUser1.getId())
                .role(Membership.TenantRole.OWNER)
                .status(Membership.MembershipStatus.ACTIVE)
                .build());

        mockMvc.perform(post("/api/auth/tenants/" + tenant.getId() + "/switch")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Changement d'entreprise (Switch) : Succès pour un utilisateur appartenant à deux entreprises")
    void shouldSwitchSuccessfullyBetweenTenants() throws Exception {
        Tenant tenantA = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name("Company A")
                .slug("comp-a-" + UUID.randomUUID().toString().substring(0, 6))
                .status(Tenant.TenantStatus.ACTIVE)
                .build());

        Tenant tenantB = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name("Company B")
                .slug("comp-b-" + UUID.randomUUID().toString().substring(0, 6))
                .status(Tenant.TenantStatus.ACTIVE)
                .build());

        membershipRepository.save(Membership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantA.getId())
                .userId(testUser1.getId())
                .role(Membership.TenantRole.OWNER)
                .status(Membership.MembershipStatus.ACTIVE)
                .build());

        membershipRepository.save(Membership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantB.getId())
                .userId(testUser1.getId())
                .role(Membership.TenantRole.SELLER)
                .status(Membership.MembershipStatus.ACTIVE)
                .build());

        // Switch vers Tenant B
        var result = mockMvc.perform(post("/api/auth/tenants/" + tenantB.getId() + "/switch")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantB.getId().toString()))
                .andExpect(jsonPath("$.tenantRole").value("SELLER"))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn();

        String newToken = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        assertEquals(tenantB.getId(), jwtService.extractTenantId(newToken));
        assertEquals("SELLER", jwtService.extractTenantRole(newToken));
    }

    @Test
    @DisplayName("Gestion des membres : OWNER peut inviter un membre et lister les membres")
    void shouldAllowOwnerToManageMembers() throws Exception {
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .id(UUID.randomUUID())
                .name("Team Corp")
                .slug("team-corp-" + UUID.randomUUID().toString().substring(0, 6))
                .status(Tenant.TenantStatus.ACTIVE)
                .build());

        membershipRepository.save(Membership.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant.getId())
                .userId(testUser1.getId())
                .role(Membership.TenantRole.OWNER)
                .status(Membership.MembershipStatus.ACTIVE)
                .build());

        String addPayload = """
                {
                    "email": "%s",
                    "role": "MANAGER"
                }
                """.formatted(testUser2.getEmail());

        mockMvc.perform(post("/api/auth/tenants/" + tenant.getId() + "/members")
                        .header("Authorization", "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(testUser2.getEmail()))
                .andExpect(jsonPath("$.role").value("MANAGER"));

        mockMvc.perform(get("/api/auth/tenants/" + tenant.getId() + "/members")
                        .header("Authorization", "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
