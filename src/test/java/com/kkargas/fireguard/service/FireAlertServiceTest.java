package com.kkargas.fireguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.kkargas.fireguard.dto.SimulationResult;
import com.kkargas.fireguard.entity.FireAlert;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.enums.AlertStatus;
import com.kkargas.fireguard.repository.FireAlertRepository;

class FireAlertServiceTest {
    private FireAlertRepository repository;
    private FireAlertService service;
    private Location location;
    private static final LocalDateTime NOW = LocalDateTime.of(2026,10,30,13,0);

    @BeforeEach
    void setUp() {
        repository = mock(FireAlertRepository.class);
        service = new FireAlertService(repository);
        location = new Location();
        ReflectionTestUtils.setField(location, "id", 7L);
    }

    private SimulationResult result(boolean fire, LocalDateTime at) {
        return new SimulationResult(7L, 3L, fire ? 80 : 20, fire ? 180 : 40,
                .997, .997, .994009, fire, at);
    }

    @Test
    void createsAnAlertOnFirstDetection() {
        when(repository.findFirstByLocationIdAndStatusOrderByDetectedAtDesc(7L, AlertStatus.ACTIVE))
                .thenReturn(Optional.empty());
        service.process(location, result(true, NOW));
        ArgumentCaptor<FireAlert> captor = ArgumentCaptor.forClass(FireAlert.class);
        verify(repository).save(captor.capture());
        assertEquals(AlertStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(80, captor.getValue().getTemperature());
        assertEquals(NOW, captor.getValue().getDetectedAt());
    }

    @Test
    void repeatedDetectionDoesNotCreateDuplicateAlert() {
        FireAlert active = new FireAlert(location, result(true, NOW));
        when(repository.findFirstByLocationIdAndStatusOrderByDetectedAtDesc(7L, AlertStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        service.process(location, result(true, NOW.plusMinutes(1)));
        verify(repository, never()).save(any());
    }

    @Test
    void normalReadingResolvesExistingAlert() {
        FireAlert active = new FireAlert(location, result(true, NOW));
        when(repository.findFirstByLocationIdAndStatusOrderByDetectedAtDesc(7L, AlertStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        service.process(location, result(false, NOW.plusMinutes(1)));
        assertEquals(AlertStatus.RESOLVED, active.getStatus());
        assertEquals(NOW.plusMinutes(1), active.getResolvedAt());
        verify(repository).save(active);
    }

    @Test
    void normalReadingWithoutActiveAlertDoesNothing() {
        when(repository.findFirstByLocationIdAndStatusOrderByDetectedAtDesc(7L, AlertStatus.ACTIVE))
                .thenReturn(Optional.empty());
        service.process(location, result(false, NOW));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMismatchedLocation() {
        SimulationResult wrong = new SimulationResult(99L,3,80,180,.99,.99,.98,true,NOW);
        assertThrows(IllegalArgumentException.class, () -> service.process(location, wrong));
        verifyNoInteractions(repository);
    }

    @Test
    void alertCannotBeResolvedTwiceOrBeforeDetection() {
        FireAlert alert = new FireAlert(location, result(true, NOW));
        assertThrows(IllegalArgumentException.class, () -> alert.resolve(NOW.minusSeconds(1)));
        alert.resolve(NOW.plusMinutes(2));
        assertThrows(IllegalStateException.class, () -> alert.resolve(NOW.plusMinutes(3)));
    }
}
