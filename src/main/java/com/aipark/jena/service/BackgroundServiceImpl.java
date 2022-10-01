package com.aipark.jena.service;

import com.aipark.jena.domain.*;
import com.aipark.jena.dto.RequestBackground;
import com.aipark.jena.dto.Response;
import com.aipark.jena.dto.ResponseBackground;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BackgroundServiceImpl implements BackgroundService {

    private final BackgroundRepository backgroundRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final Response response;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    public String bucket;

    //배경 업로드
    @Override
    public ResponseEntity<Response.Body> backgroundUpload(Long projectId,RequestBackground.BackgroundUploadDto backgroundUploadDto)throws IOException {

        Project project = projectRepository.findById(projectId).orElse(null);
        assert project != null;

        InputStream inputStream = backgroundUploadDto.getBackgroundFile().getInputStream();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        String fileName = "background/" + UUID.randomUUID().toString().toLowerCase() + ".png";

        amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata));
        inputStream.close();

        String backgroundFileUrl = "https://jenapark.s3.ap-northeast-2.amazonaws.com/" + fileName;

        // bgName
        String bgName = fileName;
        //member
        Long memberId = project.getMember().getId();
        Member member = memberRepository.findById(memberId).orElseThrow();

        Background background = Background.builder()
                .bgName(bgName)
                .isUpload(true)
                .bgUrl(backgroundFileUrl)
                .member(member)
                .build();

        backgroundRepository.save(background);

        return response.success(backgroundFileUrl,"배경 업로드를 성공했습니다.",HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Response.Body> backgroundSelect(Long bgId) {
        Background background = backgroundRepository.findById(bgId).orElseThrow();
        ResponseBackground responseBackground = new ResponseBackground(background.getId(),background.getBgName(),background.getBgUrl());

        return response.success(responseBackground.getBgUrl(),responseBackground.getBgName()+" 배경을 선택했습니다.",HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Response.Body> backgroundList(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        List<Background> backgroundListDefault = backgroundRepository.findAllByIsUpload(false);
        List<Background> backgroundListMember = backgroundRepository.findAllByMember(member);

        return response.success(responseBackgroundList(backgroundListDefault,backgroundListMember),"배경화면 리스트입니다.", HttpStatus.OK);
    }

    public List<List<ResponseBackground>> responseBackgroundList(List<Background> b1, List<Background> b2){

        List<ResponseBackground> responseBackgroundListDefault = new ArrayList<>();
        List<ResponseBackground> responseBackgroundListMember = new ArrayList<>();
        List<List<ResponseBackground>> responseBackgroundList = new ArrayList<>();

        for (int index = 0; index < b1.size(); index++) {
            Background background = b1.get(index);
            ResponseBackground responseBackground = new ResponseBackground(
                    background.getId(),
                    background.getBgName(),
                    background.getBgUrl()
            );
            responseBackgroundListDefault.add(responseBackground);
        }

        for (int index = 0; index < b2.size(); index++) {
            Background background = b2.get(index);
            ResponseBackground responseBackground = new ResponseBackground(
                    background.getId(),
                    background.getBgName(),
                    background.getBgUrl()
            );
            responseBackgroundListMember.add(responseBackground);
        }

        responseBackgroundList.add(responseBackgroundListDefault);
        responseBackgroundList.add(responseBackgroundListMember);

        return responseBackgroundList;
    }
}
