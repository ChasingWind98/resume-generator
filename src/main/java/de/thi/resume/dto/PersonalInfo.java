package de.thi.resume.dto;

public record PersonalInfo(
        String name,
        String title,
        String email,
        String adresse,
        String telefon,
        //简介部分
        String ueberMichText
) {
}
