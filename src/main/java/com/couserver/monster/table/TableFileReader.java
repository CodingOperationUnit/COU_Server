package com.couserver.monster.table;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;

final class TableFileReader {

    private TableFileReader() { }

    static byte[] read(String path) {
        try {
            return new ClassPathResource(path).getContentAsByteArray();
        } catch (IOException e) {
            // 파일이 없으면 서버 시작 자체를 실패시킨다 (Fail-Fast)
            throw new UncheckedIOException(path + " 파일을 읽을 수 없습니다.", e);
        }
    }
}