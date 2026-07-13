package com.example.fridgeapp.foodmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FoodMasterServiceTest {

  @Mock private FoodMasterRepository foodMasterRepository;

  private FoodMasterService foodMasterService;

  @BeforeEach
  void setUp() {
    foodMasterService = new FoodMasterService(foodMasterRepository);
  }

  @Test
  void searchReturnsEmptyListWithoutQueryingRepositoryWhenQueryIsBlank() {
    List<FoodMasterResponse> result = foodMasterService.search("   ");

    assertThat(result).isEmpty();
    verify(foodMasterRepository, never())
        .findTop20ByActiveTrueAndNameKanaStartingWithOrderByNameKanaAsc(eq(""));
  }

  @Test
  void searchReturnsEmptyListWhenQueryIsNull() {
    List<FoodMasterResponse> result = foodMasterService.search(null);

    assertThat(result).isEmpty();
  }

  @Test
  void searchTrimsQueryAndMapsMatchesToResponses() {
    FoodMaster foodMaster = foodMasterWith("玉ねぎ", "たまねぎ", "野菜", "個");
    when(foodMasterRepository.findTop20ByActiveTrueAndNameKanaStartingWithOrderByNameKanaAsc("たま"))
        .thenReturn(List.of(foodMaster));

    List<FoodMasterResponse> result = foodMasterService.search("  たま  ");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("玉ねぎ");
    assertThat(result.get(0).defaultCategory()).isEqualTo("野菜");
    assertThat(result.get(0).defaultUnit()).isEqualTo("個");
  }

  private FoodMaster foodMasterWith(
      String name, String nameKana, String defaultCategory, String defaultUnit) {
    FoodMaster foodMaster = new FoodMaster();
    ReflectionTestUtils.setField(foodMaster, "name", name);
    ReflectionTestUtils.setField(foodMaster, "nameKana", nameKana);
    ReflectionTestUtils.setField(foodMaster, "defaultCategory", defaultCategory);
    ReflectionTestUtils.setField(foodMaster, "defaultUnit", defaultUnit);
    return foodMaster;
  }
}
