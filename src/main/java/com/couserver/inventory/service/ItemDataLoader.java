package com.couserver.inventory.service;

import com.couserver.inventory.dto.ItemData;
import com.couserver.inventory.dto.ItemDataFile;
import com.couserver.inventory.entity.EquipSlotType;
import com.couserver.inventory.entity.Item;
import com.couserver.inventory.entity.ItemGrade;
import com.couserver.inventory.repository.ItemRepository;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ItemDataLoader implements ApplicationRunner {

    private final ItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (InputStream in = new ClassPathResource("data/Item.json").getInputStream()) {
            ItemDataFile file = objectMapper.readValue(in, ItemDataFile.class);

            for (ItemData data : file.datas()) {
                if (itemRepository.existsById(data.itemId())) {
                    continue; // 이미 있으면 건너뜀
                }
                itemRepository.save(new Item(
                        data.itemId(),
                        data.itemName(),
                        data.description(),
                        EquipSlotType.valueOf(data.slotType().toUpperCase()),
                        ItemGrade.valueOf(data.grade().toUpperCase()),
                        data.hpBonus(),
                        data.attackBonus(),
                        data.moveSpeedBonus()));
            }
        }
    }
}