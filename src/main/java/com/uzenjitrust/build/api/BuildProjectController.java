package com.uzenjitrust.build.api;

import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.service.BuildProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/build/projects")
@Tag(name = "Build Projects")
public class BuildProjectController {

    private final BuildProjectService projectService;

    public BuildProjectController(BuildProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "List projects visible to current actor")
    public ResponseEntity<Page<ProjectEntity>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(projectService.listVisible(page, size, sortBy, direction));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project visible to current actor")
    public ResponseEntity<ProjectEntity> get(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getVisibleById(projectId));
    }

    @PostMapping
    @Operation(summary = "Create draft project and construction escrow")
    public ResponseEntity<ProjectEntity> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.createDraft(request));
    }

    @PostMapping("/{projectId}/assign")
    @Operation(summary = "Assign contractor and inspector")
    public ResponseEntity<ProjectEntity> assign(@PathVariable UUID projectId,
                                                @Valid @RequestBody AssignParticipantsRequest request) {
        return ResponseEntity.ok(projectService.assignParticipants(projectId, request));
    }

    @PostMapping("/{projectId}/activate")
    @Operation(summary = "Activate project")
    public ResponseEntity<ProjectEntity> activate(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.activate(projectId));
    }
}
