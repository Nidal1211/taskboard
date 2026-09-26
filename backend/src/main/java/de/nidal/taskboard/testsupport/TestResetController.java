package de.nidal.taskboard.testsupport;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class TestResetController {

    @PostMapping("/test/reset")
    public ResponseEntity<Void> reset() {

        return ResponseEntity.noContent().build();
    }
}