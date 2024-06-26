package ru.buisnesslogiclab1.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.buisnesslogiclab1.config.HeaderConstant;
import ru.buisnesslogiclab1.dto.Response;
import ru.buisnesslogiclab1.dto.StatusCode;
import ru.buisnesslogiclab1.dto.TextDto;
import ru.buisnesslogiclab1.entity.CommentEntity;
import ru.buisnesslogiclab1.entity.LikeEntity;
import ru.buisnesslogiclab1.entity.VideoEntity;
import ru.buisnesslogiclab1.entity.VideoIdSubscriberIdPair;
import ru.buisnesslogiclab1.repository.CommentRepository;
import ru.buisnesslogiclab1.repository.LikeRepository;
import ru.buisnesslogiclab1.repository.VideoRepository;
import ru.buisnesslogiclab1.service.UserService;
import ru.buisnesslogiclab1.util.ResponseHelper;
import ru.buisnesslogiclab1.validation.IdValidator;
import ru.buisnesslogiclab1.validation.subscriber.ValidSubscriberId;
import ru.buisnesslogiclab1.validation.video.ValidVideoId;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/subscriber")
public class SubscriberController {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final ResponseHelper responseHelper;
    private final IdValidator idValidator;
    private final UserService userService;

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    @GetMapping("/video/{id}")
    public ResponseEntity<ByteArrayResource> getVideo(@PathVariable @ValidVideoId String id) {
        if (!idValidator.isIdExisting(id, null))
            return ResponseEntity.status(404).build();

        VideoEntity videoEntity = videoRepository.findById(UUID.fromString(id)).get();

        byte[] videoContent = videoEntity.getContentMP4();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/mp4"));
        headers.setContentDispositionFormData("inline", "video.mp4");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(videoContent.length)
                .body(new ByteArrayResource(videoContent));
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    @GetMapping("/video/getComments")
    public ResponseEntity<Response<List<CommentEntity>>> getComments(
            @RequestHeader(value = HeaderConstant.VIDEO_ID, required = true)
            @ValidVideoId
            String videoId) {
        if (!idValidator.isIdExisting(videoId, null))
            return responseHelper.asResponseEntity(idValidator.createErrorStatus(videoId, null));

        var videoUUID = UUID.fromString(videoId);
        var videoEntityList = commentRepository.findByVideoId(videoUUID);

        return responseHelper.asResponseEntity(StatusCode.OK, videoEntityList);
    }


    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    @PostMapping("/addLike")
    public ResponseEntity<Response<Void>> addLike(
            @RequestHeader(value = HeaderConstant.VIDEO_ID, required = true)
            @ValidVideoId
            String videoId) {
        var user = userService.findUserEntityForCurrentSession();
        if (user == null)
            return responseHelper.asResponseEntity(StatusCode.THERE_IS_NO_SUCH_USER);

        if (!idValidator.isIdExisting(videoId, null))
            return responseHelper.asResponseEntity(idValidator.createErrorStatus(videoId, null));

        if (!videoRepository.existsById(UUID.fromString(videoId)))
            return responseHelper.asResponseEntity(StatusCode.THERE_IS_NO_SUCH_VIDEO);

        likeRepository.save(new LikeEntity(user.getId(), UUID.fromString(videoId)));

        return responseHelper.asResponseEntity(StatusCode.OK);
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    @PostMapping("/revokeLike")
    public ResponseEntity<Response<Void>> revokeLike(
            @RequestHeader(value = HeaderConstant.VIDEO_ID, required = true)
            @ValidVideoId
            String videoId) {
        var user = userService.findUserEntityForCurrentSession();
        if (user == null)
            return responseHelper.asResponseEntity(StatusCode.THERE_IS_NO_SUCH_USER);

        if (!idValidator.isIdExisting(videoId, null))
            return responseHelper.asResponseEntity(idValidator.createErrorStatus(videoId, null));

        if (!videoRepository.existsById(UUID.fromString(videoId)))
            return responseHelper.asResponseEntity(StatusCode.THERE_IS_NO_SUCH_VIDEO);

        var id = new VideoIdSubscriberIdPair(user.getId(), UUID.fromString(videoId));
        if (likeRepository.existsById(id))
            likeRepository.deleteById(id);

        return responseHelper.asResponseEntity(StatusCode.OK);
    }

    @PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    @PostMapping("/addComment")
    public ResponseEntity<Response<Void>> addComment(
            @RequestHeader(value = HeaderConstant.VIDEO_ID, required = true)
            @ValidVideoId
            String videoId,
            @RequestBody
            TextDto textDto) {
        var user = userService.findUserEntityForCurrentSession();
        if (user == null)
            return responseHelper.asResponseEntity(StatusCode.THERE_IS_NO_SUCH_USER);

        if (!idValidator.isIdExisting(videoId, null))
            return responseHelper.asResponseEntity(idValidator.createErrorStatus(videoId, null));

        if (!videoRepository.existsById(UUID.fromString(videoId)))
            return responseHelper.asResponseEntity(StatusCode.THERE_IS_NO_SUCH_VIDEO);

        commentRepository.save(new CommentEntity(user.getId(), UUID.fromString(videoId), textDto.getText()));

        return responseHelper.asResponseEntity(StatusCode.OK);
    }


}
