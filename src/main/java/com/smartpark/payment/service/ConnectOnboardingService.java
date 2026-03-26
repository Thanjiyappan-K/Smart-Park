package com.smartpark.payment.service;

import com.smartpark.payment.dto.ConnectOnboardResponse;
import com.smartpark.payment.exception.PaymentErrorCode;
import com.smartpark.payment.exception.PaymentException;
import com.smartpark.user.entity.User;
import com.smartpark.user.repository.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectOnboardingService {

    private final StripeService stripeService;
    private final UserRepository userRepository;

    /**
     * If user already has stripe_account_id, create new account link and return URL.
     * Otherwise create Express account, save to user, then create account link.
     */
    @Transactional
    public ConnectOnboardResponse getOrCreateOnboardingUrl(User user) {
        String accountId = user.getStripeAccountId();
        if (accountId != null && !accountId.isBlank()) {
            try {
                String url = stripeService.createAccountLink(accountId);
                return ConnectOnboardResponse.builder()
                        .url(url)
                        .accountId(accountId)
                        .build();
            } catch (StripeException e) {
                log.error("Failed to create account link for user {}", user.getId(), e);
                throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, e.getMessage());
            }
        }
        try {
            Account account = stripeService.createConnectExpressAccount(user.getEmail(), "IN");
            accountId = account.getId();
            user.setStripeAccountId(accountId);
            userRepository.save(user);
            String url = stripeService.createAccountLink(accountId);
            return ConnectOnboardResponse.builder()
                    .url(url)
                    .accountId(accountId)
                    .build();
        } catch (StripeException e) {
            log.error("Failed to create Connect account for user {}", user.getId(), e);
            throw new PaymentException(PaymentErrorCode.STRIPE_ERROR, e.getMessage());
        }
    }
}
