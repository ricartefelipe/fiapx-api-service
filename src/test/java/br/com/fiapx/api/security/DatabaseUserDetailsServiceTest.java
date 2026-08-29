package br.com.fiapx.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.fiapx.api.domain.User;
import br.com.fiapx.api.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService databaseUserDetailsService;

    @Test
    void shouldLoadUserByUsername() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getUsername()).thenReturn("fiapx");
        when(user.getPasswordHash()).thenReturn("hash");
        when(userRepository.findByUsername("fiapx")).thenReturn(Optional.of(user));

        var details = databaseUserDetailsService.loadUserByUsername("fiapx");

        assertThat(details.getUsername()).isEqualTo("fiapx");
        assertThat(((FiapxUserDetails) details).getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> databaseUserDetailsService.loadUserByUsername("inexistente"))
            .isInstanceOf(UsernameNotFoundException.class);
    }
}
