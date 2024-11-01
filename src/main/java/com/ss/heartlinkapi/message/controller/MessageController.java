package com.ss.heartlinkapi.message.controller;

import com.ss.heartlinkapi.login.dto.CustomUserDetails;
import com.ss.heartlinkapi.message.dto.*;
import com.ss.heartlinkapi.message.service.MessageRoomService;
import com.ss.heartlinkapi.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dm")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageRoomService messageRoomService;
    private final MessageService messageService;

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("images").resolve(filename); // 'images'는 루트 디렉토리에 있는 폴더
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    //    메세지와 관련된 모든 정보 가져오기
    @GetMapping
    public ResponseEntity<HashMap<String, Object>> getAllChatList(@AuthenticationPrincipal CustomUserDetails user) {

        HashMap<String, Object> response = new HashMap<>();
        response.put("MyLoginId", user.getUsername());
        response.put("MyUserId", user.getUserId());
        List<Object> chatList = messageRoomService.getAllChatList(user.getUserId());
        response.put("chatList", chatList);

        return ResponseEntity.ok(response);
    }

    //    텍스트 메세지를 저장
    @PostMapping("/messages/text")
    public ResponseEntity<String> createTextMessage(@RequestBody TextMessageDTO textMessageDTO) {

        SaveMsgDTO saveMsgDTO = new SaveMsgDTO().builder()
                .msgRoomId(textMessageDTO.getMsgRoomId())
                .senderId(textMessageDTO.getSenderId())
                .content(textMessageDTO.getContent())
                .messageTime(LocalDateTime.now())
                .isRead(false)
                .build();

        messageService.saveChatMessage(saveMsgDTO);

        return ResponseEntity.ok("save message");
    }

    //    이미지 파일 또는 gif를 메시지 저장
    @PostMapping("/messages/img")
    public ResponseEntity<String> saveImageMessage(@RequestParam("file") MultipartFile multipartFile,
                                                       @RequestParam("msgRoomId") Long msgRoomId,
                                                       @RequestParam("senderId") Long senderId) {

//        이미지가 비었는지 확인
        if (multipartFile.isEmpty()) {
            return ResponseEntity.badRequest().body("no file");

        } else {
            try {

//                파일 확장자가 이미지 계열인지 확인
                String fileExtension = multipartFile.getOriginalFilename() != null
                        ? multipartFile.getOriginalFilename().substring(multipartFile.getOriginalFilename().lastIndexOf("."))
                        : "";

                if (!fileExtension.matches("(?i)\\.(jpg|jpeg|png|gif)$")) {
                    return ResponseEntity.badRequest().body("not supported");
                }

//            현재 heartlink-api폴더 경로를 가져옴.
                String currentPath = Paths.get("").toAbsolutePath().toString();

//            img파일 위치 경로에 파일 이름을 더해 filePath에 저장
                String newFileName = UUID.randomUUID().toString() + fileExtension;
                String filePath = currentPath + "/images/" + newFileName;


                multipartFile.transferTo(new File(filePath));

//            이미지를 가져올 경로를 저장하는 과정
                String importPath = "http://localhost:9090/dm/images/" + newFileName;

                SaveMsgDTO saveMsgDTO = new SaveMsgDTO().builder()
                        .msgRoomId(msgRoomId)
                        .senderId(senderId)
                        .emoji(null)
                        .imageUrl(importPath)
                        .messageTime(LocalDateTime.now())
                        .isRead(false)
                        .build();

                messageService.saveChatMessage(saveMsgDTO);

                return ResponseEntity.ok(importPath);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return ResponseEntity.ok("save good");
    }

    //    비공개 사용자에게 메시지 요청보내기
    @PostMapping("/message/apply")
    public ResponseEntity<String> applyMessage(@RequestBody ApplyMessageDTO applyMessageDTO) {

        messageRoomService.applyMessage(applyMessageDTO);

        return ResponseEntity.ok("apply success");
    }

    //    비공개 사용자 메세지 요청 거절
    @GetMapping("/message/rejection/{msgRoomId}")
    public ResponseEntity<String> applyRejection(@PathVariable("msgRoomId") Long msgRoomId) {

        messageRoomService.applyRejection(msgRoomId);

        return ResponseEntity.ok("rejection success");
    }

    //    비공개 사용자 메시지 요청 수락
    @PutMapping("/message/accept/{msgRoomId}")
    public ResponseEntity<String> applyAccept(@PathVariable("msgRoomId") Long msgRoomId) {

        messageRoomService.applyAccept(msgRoomId);

        return ResponseEntity.ok("accept success");
    }

    //    사용자가 타 사용자를 차단한 경우 사용자는 타 사용자에게 DM을 보낼 수 없다
    @GetMapping("/message/block")
    public ResponseEntity<String> blockMessage(@RequestBody BlockUserCheckDTO blockUserCheckDTO) {

        boolean result = false;
        result = messageService.blockMessage(blockUserCheckDTO);

        if (result)
            return ResponseEntity.ok("block user");
        else
            return ResponseEntity.ok("nonblock user");
    }
}
