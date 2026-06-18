package com.sb.sfrigola_core.config.auditor;

import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(SCAuthenticationUtils.getAuthUser());
    }
}
