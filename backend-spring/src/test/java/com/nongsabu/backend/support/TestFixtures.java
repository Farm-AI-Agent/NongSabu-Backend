package com.nongsabu.backend.support;

import com.nongsabu.backend.domain.crop.entity.Crop;
import com.nongsabu.backend.domain.farm.entity.Farm;
import com.nongsabu.backend.domain.farmprofile.entity.ExperienceLevel;
import com.nongsabu.backend.domain.farmprofile.entity.FarmProfile;
import com.nongsabu.backend.domain.image.entity.AnalysisStatus;
import com.nongsabu.backend.domain.image.entity.ImageAnalysisResult;
import com.nongsabu.backend.domain.image.entity.UploadedImage;
import com.nongsabu.backend.domain.member.entity.Member;
import com.nongsabu.backend.domain.member.entity.MemberRole;
import com.nongsabu.backend.domain.usercrop.entity.UserCrop;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Member member(Long id) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .password("encoded-password")
                .name("member-" + id)
                .role(MemberRole.USER)
                .build();
    }

    public static Crop crop(Long id, String name) {
        return Crop.builder()
                .id(id)
                .name(name)
                .category("category")
                .description("description")
                .build();
    }

    public static Farm farm(Long id, Member member) {
        return Farm.builder()
                .id(id)
                .member(member)
                .name("farm-" + id)
                .location("Naju")
                .cultivationArea("greenhouse")
                .cropSummary("grape")
                .notes("notes")
                .build();
    }

    public static FarmProfile farmProfile(Long id, Member member) {
        return FarmProfile.builder()
                .id(id)
                .member(member)
                .region("Naju")
                .experienceLevel(ExperienceLevel.BEGINNER)
                .farmSize("small")
                .mainCrop("grape")
                .build();
    }

    public static UserCrop userCrop(Long id, Member member, Crop crop) {
        return UserCrop.builder()
                .id(id)
                .member(member)
                .crop(crop)
                .cultivationArea("greenhouse-1")
                .memo("memo")
                .build();
    }

    public static UploadedImage uploadedImage(Long id, Member member, Crop crop, AnalysisStatus status) {
        return UploadedImage.builder()
                .id(id)
                .member(member)
                .crop(crop)
                .originalFilename("leaf.jpg")
                .storagePath("/uploads/images/leaf.jpg")
                .contentType("image/jpeg")
                .fileSize(10L)
                .analysisStatus(status)
                .build();
    }

    public static ImageAnalysisResult imageAnalysisResult(Long id, UploadedImage image) {
        return ImageAnalysisResult.builder()
                .id(id)
                .uploadedImage(image)
                .diseaseName("Grape disease suspicion")
                .confidence(0.87)
                .severity("LOW")
                .summary("summary")
                .recommendation("recommendation")
                .rawResponse("{}")
                .build();
    }
}
