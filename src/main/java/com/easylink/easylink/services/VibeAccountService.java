package com.easylink.easylink.services;

import com.easylink.easylink.dtos.*;
import com.easylink.easylink.entities.AssociativeEntry;
import com.easylink.easylink.entities.QuestionTemplate;
import com.easylink.easylink.entities.VibeAccount;
import com.easylink.easylink.exceptions.DuplicateAccountException;
import com.easylink.easylink.exceptions.IncorrectAnswerException;
import com.easylink.easylink.exceptions.UserLockedException;
import com.easylink.easylink.repositories.AssociativeEntryRepository;
import com.easylink.easylink.repositories.QuestionTemplateRepository;
import com.easylink.easylink.repositories.VibeAccountRepository;
import com.easylink.easylink.vibe_service.application.service.AmplitudeService;
import com.easylink.easylink.services.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VibeAccountService {

    private final VibeAccountRepository vibeAccountRepository;
    private final AssociativeEntryRepository associativeEntryRepository;
    private final QuestionTemplateRepository questionTemplateRepository;
    private final AmplitudeService amplitudeService;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    private final JwtService jwtService;

    private final EmailVerificationService emailVerificationService;

    private AssociativeEntry createAssociativeEntry(AssociativeEntryDTO dto) {

        AssociativeEntry associativeEntry = modelMapper.map(dto, AssociativeEntry.class);

        String text = dto.getRealQuestion().getText();
        boolean isPredefined = dto.getRealQuestion().isPredefined();


        QuestionTemplate questionTemplate = questionTemplateRepository.findByText(text)
                .orElseGet(() -> {
                    QuestionTemplate qt = new QuestionTemplate();
                    qt.setText(text);
                    qt.setPredefined(isPredefined);
                    qt.setCreatedAt(LocalDateTime.now());
                    return questionTemplateRepository.save(qt);
                });

        String hashedAnswer = passwordEncoder.encode(dto.getAnswer());

        associativeEntry.setRealQuestion(questionTemplate);
        associativeEntry.setAnswerHash(hashedAnswer);

        return associativeEntry;
    }


    public boolean createVibeAccount(SignUpDTO signUpDTO){
        List<VibeAccount> existing = vibeAccountRepository.findByEmail(signUpDTO.getEmail());

        if(existing.stream().count()>1){
            throw new DuplicateAccountException("More than one account with this email!");
        }

          if (!existing.isEmpty()) {
            VibeAccount account = existing.get(0);
            if (Boolean.TRUE.equals(account.getIsEmailVerified())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "signup.account_already_exists");
            }

            if (account.getTokenExpiry() != null && account.getTokenExpiry().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signup.verification_already_sent");
            }
            vibeAccountRepository.delete(account);
        }

        boolean accountExists = vibeAccountRepository.existsByEmail(signUpDTO.getEmail());

        if(accountExists){
            throw new IllegalStateException("Vibe account with this email already exists");
        }

        VibeAccount vibeAccount = new VibeAccount();
        vibeAccount.setCreated(LocalDateTime.now());
        vibeAccount.setLastLogin(LocalDateTime.now());
        vibeAccount.setEmail(signUpDTO.getEmail());

        List<AssociativeEntry> entries = signUpDTO.getEntries()
                .stream()
                .map(this::createAssociativeEntry)
                .collect(Collectors.toList());

        entries.forEach(e -> e.setVibeAccount(vibeAccount));

        vibeAccount.setAssociativeEntries(entries);

        amplitudeService.sendEvent(signUpDTO.getEmail(),"User created", Map.of(
                "source", "backend",
                "email",signUpDTO.getEmail()
        ));

        VibeAccount savedAccount = vibeAccountRepository.save(vibeAccount);
        emailVerificationService.sendVerificationEmail(savedAccount);

        return savedAccount != null;
    }

    public boolean createPasswordAccount(PasswordSignUpDTO dto) {
        List<VibeAccount> existing = vibeAccountRepository.findByEmail(dto.getEmail());

        if (existing.size() > 1) throw new DuplicateAccountException("More than one account with this email!");

        if (!existing.isEmpty()) {
            VibeAccount acc = existing.get(0);

            if (Boolean.TRUE.equals(acc.getIsEmailVerified())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "signup.account_already_exists");
            }

            if (acc.getTokenExpiry() != null && acc.getTokenExpiry().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signup.verification_already_sent");
            }

            vibeAccountRepository.delete(acc);
        }

        VibeAccount vibeAccount = new VibeAccount();
        vibeAccount.setCreated(LocalDateTime.now());
        vibeAccount.setLastLogin(LocalDateTime.now());
        vibeAccount.setEmail(dto.getEmail());

        vibeAccount.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        amplitudeService.sendEvent(dto.getEmail(), "User created", Map.of(
                "source", "backend",
                "email", dto.getEmail(),
                "signup_method", "password"
        ));

        VibeAccount saved = vibeAccountRepository.save(vibeAccount);
        emailVerificationService.sendVerificationEmail(saved);

        return saved != null;
    }



    private List<AssociativeEntry> getRandomQuestionByEmail(String email){

        List<VibeAccount> listVibe = vibeAccountRepository.findByEmail(email);

        if(listVibe.stream().count()>1){
            throw new DuplicateAccountException("More than one account with this email!");
        }

        if(listVibe.isEmpty()) {
            throw new IllegalStateException("No one account with this email!");
        };

        List<AssociativeEntry> associativeEntryList = listVibe.get(0).getAssociativeEntries();

        if(associativeEntryList==null || associativeEntryList.isEmpty()) return List.of();

        int maxCount = Math.min(3,associativeEntryList.size());

        int count = new Random().nextInt(maxCount) + 1;

        Collections.shuffle(associativeEntryList);

        //return associativeEntryList.subList(0,count);
        return associativeEntryList;
    }

    public List<AssociativeQuestionDTO> startAuth(Map<String,String> payload){

        String email = payload.get("email");

        List<AssociativeEntry> associativeEntryQuestions = getRandomQuestionByEmail(email);

        return associativeEntryQuestions.stream()
                .map(entry->new AssociativeQuestionDTO(entry.getId(),entry.getAssociativeQuestion()))
                .toList();
    }

    private void validateRequest(AssociativeLoginRequestDTO requestDTO){

        if(requestDTO == null || requestDTO.getAnswers() == null || requestDTO.getAnswers().isEmpty()){

            throw new IllegalArgumentException("Answers list must not be empty");
        }
    }

    private List<AssociativeEntry> loadEntries(AssociativeLoginRequestDTO requestDTO){
        List<UUID> uuidList = requestDTO.getAnswers().stream().map(AssociativeAnswerDTO::getEntryId).toList();

        List<AssociativeEntry> associativeEntryList = associativeEntryRepository.findAllById(uuidList);

        return associativeEntryList;
    }

    private boolean verifyAnswers(List<AssociativeEntry> associativeEntryList, AssociativeLoginRequestDTO requestDTO){

        List<String> inputAnswers = requestDTO.getAnswers().stream().map(a -> a.getAnswer().trim()).toList();

        return associativeEntryList.stream().allMatch(entry ->
                inputAnswers.stream().anyMatch(input -> passwordEncoder.matches(input, entry.getAnswerHash()))
        );
    }

    private void checkLock(List<AssociativeEntry> associativeEntryList, String timezone) {
        Instant lockTime = associativeEntryList.getFirst().getVibeAccount().getLockTime();

        if (lockTime != null) {
            Instant now = Instant.now();

            if (lockTime.isAfter(now)) {

                ZoneId userZone = ZoneId.of(timezone);
                ZonedDateTime userLockTime = lockTime.atZone(userZone);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");
                String formattedLockTime = userLockTime.format(formatter);

                throw new UserLockedException("Account is locked until " + formattedLockTime + " (" + timezone + ")");
            }
        }
    }

    public String checkPassword(PasswordLoginDTO dto) {
        List<VibeAccount> list = vibeAccountRepository.findByEmail(dto.getEmail());

        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (list.size() > 1) throw new DuplicateAccountException("More than one account with this email!");

        VibeAccount acc = list.get(0);

        if (Boolean.FALSE.equals(acc.getIsEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please verify your email before logging in.");
        }

        if (acc.getPasswordHash() == null || acc.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password login is not enabled for this account.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), acc.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        amplitudeService.sendEvent(dto.getEmail(), "User login", Map.of(
                "source", "backend",
                "email", dto.getEmail(),
                "login_method", "password"
        ));

        return dto.getEmail();
    }



    private void handleFailedLogin(VibeAccount vibeAccount) {
        vibeAccount.setFailedAttempts(vibeAccount.getFailedAttempts() + 1);

        if (vibeAccount.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            vibeAccount.setLockTime(Instant.now().plus(LOCK_DURATION));
        }

        vibeAccountRepository.save(vibeAccount);
    }



    private void checkAnswers(List<AssociativeEntry> associativeEntryList,AssociativeLoginRequestDTO associativeLoginRequestDTO){

        if(!verifyAnswers(associativeEntryList,associativeLoginRequestDTO)){

            VibeAccount vibeAccount = associativeEntryList.get(0).getVibeAccount();

            handleFailedLogin(vibeAccount);

            throw new IncorrectAnswerException("Your answers are incorrect.");

        }
    }

    public String checkAnswers(AssociativeLoginRequestDTO associativeLoginRequestDTO){

        amplitudeService.sendEvent(associativeLoginRequestDTO.getEmail(),"User login", Map.of(
                "source", "backend",
                "email",associativeLoginRequestDTO.getEmail()
        ));

        validateRequest(associativeLoginRequestDTO);

        List<AssociativeEntry> associativeEntryList = loadEntries(associativeLoginRequestDTO);
        VibeAccount vibeAccount = associativeEntryList.get(0).getVibeAccount();
        if (Boolean.FALSE.equals(vibeAccount.getIsEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please verify your email before logging in.");
        }

        checkLock(associativeEntryList,associativeLoginRequestDTO.getTimezone());

        checkAnswers(associativeEntryList,associativeLoginRequestDTO);

        return associativeLoginRequestDTO.getEmail();
    }
    public boolean verifyEmail(String token) {
        return emailVerificationService.verifyToken(token);
    }


    public String generateToken(String email) {
        List<VibeAccount> listAccount = vibeAccountRepository.findByEmail(email);

        if (listAccount.isEmpty()) {
            throw new UsernameNotFoundException("No account found with email: " + email);
        }

        if (listAccount.size() > 1) {
            throw new IllegalStateException("Multiple accounts found with same email: " + email);
        }

        return jwtService.generateToken(listAccount.get(0).getId().toString());
    }


    public void deleteUnverifiedAccounts() {
        List<VibeAccount> expiredAccounts = vibeAccountRepository
                .findByIsEmailVerifiedFalseAndTokenExpiryBefore(LocalDateTime.now());

        vibeAccountRepository.deleteAll(expiredAccounts);
    }
}
