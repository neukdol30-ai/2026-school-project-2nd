package com.siyan1234.itproject2nd.map.controller;

import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.map.dto.MapFavoriteActionResponseDto;
import com.siyan1234.itproject2nd.map.dto.MapFavoriteSaveRequestDto;
import com.siyan1234.itproject2nd.map.dto.MapFavoriteSaveResultDto;
import com.siyan1234.itproject2nd.map.service.MapFavoritePlaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/kakao-map/favorites")
public class MapFavoritePlaceApiController {

    private final MapFavoritePlaceService mapFavoritePlaceService;
    private final LoginMemberResolver loginMemberResolver;

    public MapFavoritePlaceApiController(MapFavoritePlaceService mapFavoritePlaceService,
                                         LoginMemberResolver loginMemberResolver) {
        this.mapFavoritePlaceService = mapFavoritePlaceService;
        this.loginMemberResolver = loginMemberResolver;
    }

    @GetMapping
    public ResponseEntity<MapFavoriteActionResponseDto> findAll() {
        Integer memberNo = loginMemberResolver.getCurrentMemberNo();
        if (memberNo == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(MapFavoriteActionResponseDto.success(
                "즐겨찾기 목록을 조회했습니다.",
                mapFavoritePlaceService.findAll(memberNo)
        ));
    }

    @PostMapping
    public ResponseEntity<MapFavoriteActionResponseDto> add(@Valid @RequestBody MapFavoriteSaveRequestDto request) {
        Integer memberNo = loginMemberResolver.getCurrentMemberNo();
        if (memberNo == null) {
            return unauthorized();
        }

        MapFavoriteSaveResultDto result = mapFavoritePlaceService.add(memberNo, request);
        String message = result.isCreated()
                ? "즐겨찾기에 추가했습니다."
                : "이미 즐겨찾기에 저장된 장소입니다.";
        HttpStatus status = result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;

        return ResponseEntity.status(status)
                .body(MapFavoriteActionResponseDto.success(message, result.getFavorite()));
    }

    @DeleteMapping("/{favoriteNo}")
    public ResponseEntity<MapFavoriteActionResponseDto> delete(@PathVariable("favoriteNo") int favoriteNo) {
        Integer memberNo = loginMemberResolver.getCurrentMemberNo();
        if (memberNo == null) {
            return unauthorized();
        }

        boolean deleted = mapFavoritePlaceService.delete(memberNo, favoriteNo);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(MapFavoriteActionResponseDto.failure("삭제할 즐겨찾기를 찾지 못했습니다."));
        }

        return ResponseEntity.ok(MapFavoriteActionResponseDto.success(
                "즐겨찾기에서 삭제했습니다.",
                null
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MapFavoriteActionResponseDto> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("즐겨찾기 입력값을 확인해주세요.");

        return ResponseEntity.badRequest().body(MapFavoriteActionResponseDto.failure(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MapFavoriteActionResponseDto> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(MapFavoriteActionResponseDto.failure(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<MapFavoriteActionResponseDto> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MapFavoriteActionResponseDto.failure(exception.getMessage()));
    }

    private ResponseEntity<MapFavoriteActionResponseDto> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MapFavoriteActionResponseDto.failure("로그인이 필요합니다."));
    }
}
