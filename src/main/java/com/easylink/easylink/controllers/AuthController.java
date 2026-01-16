package com.easylink.easylink.controllers; 

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.easylink.easylink.dtos.*;
import com.easylink.easylink.entities.VibeAccount;
import com.easylink.easylink.services.PersonService;
import com.easylink.easylink.services.QuestionTemplateService;
import com.easylink.easylink.services.VibeAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.easylink.easylink.services.Email2faService;
import com.easylink.easylink.services.RefreshTokenService;
import com.easylink.easylink.services.JwtService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v3/auth")
public class AuthController {
    private final VibeAccountService vibeAccountService;
    private final PersonService personService;
    private final QuestionTemplateService questionTemplateService;

    private final Email2faService email2faService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;


    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @PostMapping("/signup")
    public ResponseEntity<String> createVibeAccount(@RequestBody @Valid SignUpDTO signUpDTO){
        boolean created = vibeAccountService.createVibeAccount(signUpDTO);
        if (created){
            return ResponseEntity.ok("Verification email sent successfully!");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The account was not created");
    }

    @PostMapping("/start")
    public ResponseEntity<?> startAuth(@RequestBody Map<String,String> payload){
        List<AssociativeQuestionDTO> result  = vibeAccountService.startAuth(payload);
        if(result.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No questions found");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkAnswers(@RequestBody @Valid AssociativeLoginRequestDTO associativeLoginRequestDTO){
        String email = vibeAccountService.checkAnswers(associativeLoginRequestDTO);
        String token = vibeAccountService.generateToken(email);
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    @GetMapping("/question-templates")
    public ResponseEntity<List<QuestionTemplateDTO>> getAllQuestionTemplates() {
        List<QuestionTemplateDTO> questionTemplateDTOS = questionTemplateService.getAllQuestionTemplates();
        if (questionTemplateDTOS.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204
        }
        return ResponseEntity.ok(questionTemplateDTOS); // 200
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        boolean success = vibeAccountService.verifyEmail(token);
        if (success) {
            return ResponseEntity.status(302) // 302 = FOUND
                    .header("Location", frontendBaseUrl + "/email-verified")
                    .build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }
    }

    @PostMapping("/signup-password")
    public ResponseEntity<?> signupPassword(@RequestBody @Valid PasswordSignUpDTO dto) {
        boolean created = vibeAccountService.createPasswordAccount(dto);
        if (created) {
            return ResponseEntity.ok("Verification email sent successfully!");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The account was not created");
    }

    @PostMapping("/signin-password")
    public ResponseEntity<Start2faResponse> signinPassword(
            @RequestBody @Valid PasswordLoginDTO dto,
            HttpServletRequest req
    ) {
        String email = vibeAccountService.checkPassword(dto);
        VibeAccount acc = vibeAccountService.loadByEmail(email);

        UUID challengeId = email2faService.createChallenge(
                acc,
                clientIp(req),
                req.getHeader("User-Agent")
        );

        return ResponseEntity.ok(new Start2faResponse(true, challengeId));
    }

    private String clientIp(HttpServletRequest req) {
        String cf = req.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) return cf;

        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();

        return req.getRemoteAddr();
    }

    @PostMapping("/signin-password/verify-2fa")
    public ResponseEntity<TokenResponse> verify2fa(
            @RequestBody @Valid Verify2faDTO dto,
            HttpServletRequest req
    ) {
        VibeAccount acc = email2faService.verify(dto.getChallengeId(), dto.getCode());

        String access = jwtService.generateToken(acc.getId().toString());
        String refresh = refreshTokenService.issueRefreshToken(
                acc,
                dto.isRememberMe(),
                clientIp(req),
                req.getHeader("User-Agent")
        );

        return ResponseEntity.ok(new TokenResponse(access, refresh));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody @Valid RefreshRequest dto) {
        VibeAccount acc = refreshTokenService.consumeForAccess(dto.getRefreshToken());
        String access = jwtService.generateToken(acc.getId().toString());
        return ResponseEntity.ok(Map.of("accessToken", access));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody @Valid LogoutRequest dto) {
        refreshTokenService.revoke(dto.getRefreshToken());
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
