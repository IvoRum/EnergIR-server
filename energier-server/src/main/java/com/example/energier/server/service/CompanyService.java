package com.example.energier.server.service;

import com.example.energier.server.model.responce.CompanyResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.User;
import com.example.energier.server.persistence.UserCompany;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.UserCompanyRepository;
import com.example.energier.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

/**
 * Company membership.
 *
 * A user is attached to exactly one company. The company is resolved from the
 * authenticated user on every request; meter data is scoped to it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    /** Companies the authenticated user is a member of (0 or 1). */
    public List<CompanyResponse> getCompaniesForUser(String email) {
        User user = findUser(email);
        return userCompanyRepository.findByUser_IdOrderByIdAsc(user.getId()).stream()
                .map(UserCompany::getCompany)
                .map(this::toResponse)
                .toList();
    }

    /** The company the user is attached to, or null. */
    public CompanyResponse getCompanyForUser(String email) {
        User user = findUser(email);
        return userCompanyRepository.findFirstByUser_IdOrderByIdAsc(user.getId())
                .map(UserCompany::getCompany)
                .map(this::toResponse)
                .orElse(null);
    }

    /** Company id for the authenticated user; 403 if they have no company yet. */
    public Integer requireCompanyId(String email) {
        CompanyResponse company = getCompanyForUser(email);
        if (company == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This user is not attached to a company yet"
            );
        }
        return company.getId();
    }

    /** Creates the user's company. Rejected if they already belong to one. */
    public CompanyResponse createCompanyForUser(String email, String rawName) {
        User user = findUser(email);

        if (userCompanyRepository.existsByUser_Id(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This user already belongs to a company"
            );
        }

        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Company name is required");
        }

        Company company = Company.builder()
                .name(name)
                .code(generateCode())
                .createdOn(Instant.now())
                .build();
        company = companyRepository.save(company);

        userCompanyRepository.save(UserCompany.builder()
                .user(user)
                .company(company)
                .createdOn(Instant.now())
                .build());

        return toResponse(company);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String generateCode() {
        for (int attempt = 0; attempt < 25; attempt++) {
            StringBuilder code = new StringBuilder("PE-");
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            if (!companyRepository.existsByCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique company code");
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .createdOn(company.getCreatedOn())
                .build();
    }
}
