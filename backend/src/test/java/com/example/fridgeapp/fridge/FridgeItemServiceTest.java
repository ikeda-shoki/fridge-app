package com.example.fridgeapp.fridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.foodmaster.FoodMasterRepository;
import com.example.fridgeapp.group.GroupAccessGuard;
import com.example.fridgeapp.group.GroupException;
import com.example.fridgeapp.group.GroupMemberRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FridgeItemServiceTest {

  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private FridgeItemRepository fridgeItemRepository;
  @Mock private ConsumptionEventRepository consumptionEventRepository;
  @Mock private FoodMasterRepository foodMasterRepository;
  @Mock private GroupMemberRepository groupMemberRepository;

  private FridgeItemService fridgeItemService;

  @BeforeEach
  void setUp() {
    fridgeItemService =
        new FridgeItemService(
            fridgeItemRepository,
            consumptionEventRepository,
            foodMasterRepository,
            new GroupAccessGuard(groupMemberRepository));
    lenient().when(groupMemberRepository.existsMember(GROUP_ID, USER_ID)).thenReturn(true);
  }

  @Test
  void listItemsThrowsWhenUserIsNotGroupMember() {
    UUID outsiderId = UUID.randomUUID();
    when(groupMemberRepository.existsMember(GROUP_ID, outsiderId)).thenReturn(false);

    assertThatThrownBy(() -> fridgeItemService.listItems(outsiderId, GROUP_ID, null, null))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.NOT_GROUP_MEMBER);
  }

  @Test
  void listItemsPassesNoFiltersWhenCategoryAndQueryAreAbsent() {
    when(fridgeItemRepository.searchActiveByGroupId(GROUP_ID, true, null, ""))
        .thenReturn(List.of());

    List<FridgeItemResponse> result = fridgeItemService.listItems(USER_ID, GROUP_ID, null, null);

    assertThat(result).isEmpty();
  }

  @Test
  void listItemsEscapesLikeWildcardsInSearchQuery() {
    when(fridgeItemRepository.searchActiveByGroupId(
            eq(GROUP_ID), eq(false), eq(FridgeItemCategory.VEGETABLE), eq("100\\%")))
        .thenReturn(List.of());

    fridgeItemService.listItems(USER_ID, GROUP_ID, "野菜", " 100% ");

    verify(fridgeItemRepository)
        .searchActiveByGroupId(GROUP_ID, false, FridgeItemCategory.VEGETABLE, "100\\%");
  }

  @Test
  void listItemsThrowsOnUnknownCategory() {
    assertThatThrownBy(() -> fridgeItemService.listItems(USER_ID, GROUP_ID, "宇宙食", null))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(AppError.INVALID_FRIDGE_ITEM_CATEGORY);
  }

  @Test
  void createItemDefaultsPurchaseDateAndBuyerToRegistration() {
    when(fridgeItemRepository.save(any(FridgeItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    FridgeItemCreateRequest request =
        new FridgeItemCreateRequest(
            null, "玉ねぎ", new BigDecimal("2"), "個", "野菜", null, null, null, null);

    FridgeItemResponse response = fridgeItemService.createItem(USER_ID, GROUP_ID, request);

    assertThat(response.displayName()).isEqualTo("玉ねぎ");
    assertThat(response.category()).isEqualTo("野菜");
    assertThat(response.purchasedAt()).isEqualTo(LocalDate.now());
    assertThat(response.purchasedBy()).isEqualTo(USER_ID);
    assertThat(response.status()).isEqualTo(FridgeItemStatus.ACTIVE);
  }

  @Test
  void createItemThrowsWhenFoodMasterDoesNotExist() {
    UUID foodMasterId = UUID.randomUUID();
    when(foodMasterRepository.existsById(foodMasterId)).thenReturn(false);

    FridgeItemCreateRequest request =
        new FridgeItemCreateRequest(
            foodMasterId, "玉ねぎ", new BigDecimal("1"), "個", "野菜", null, null, null, null);

    assertThatThrownBy(() -> fridgeItemService.createItem(USER_ID, GROUP_ID, request))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(AppError.FOOD_MASTER_NOT_FOUND);
  }

  @Test
  void updateItemLeavesOmittedFieldsUnchanged() {
    UUID itemId = UUID.randomUUID();
    FridgeItem item = activeItem(itemId, new BigDecimal("3"));
    when(fridgeItemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(fridgeItemRepository.save(item)).thenReturn(item);

    FridgeItemUpdateRequest request =
        new FridgeItemUpdateRequest(
            null, null, new BigDecimal("5"), null, null, null, null, null, "残り少ない");

    FridgeItemResponse response = fridgeItemService.updateItem(USER_ID, itemId, request);

    assertThat(response.quantity()).isEqualByComparingTo("5");
    assertThat(response.memo()).isEqualTo("残り少ない");
    assertThat(response.displayName()).isEqualTo("玉ねぎ");
    assertThat(response.category()).isEqualTo("野菜");
  }

  @Test
  void deleteItemMarksItemDeletedInsteadOfRemovingIt() {
    UUID itemId = UUID.randomUUID();
    FridgeItem item = activeItem(itemId, new BigDecimal("1"));
    when(fridgeItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    fridgeItemService.deleteItem(USER_ID, itemId);

    assertThat(item.getStatus()).isEqualTo(FridgeItemStatus.DELETED);
    verify(fridgeItemRepository).save(item);
    verify(fridgeItemRepository, never()).delete(any());
  }

  @Test
  void deleteItemThrowsWhenItemIsAlreadyDeleted() {
    UUID itemId = UUID.randomUUID();
    FridgeItem item = activeItem(itemId, new BigDecimal("1"));
    item.markDeleted();
    when(fridgeItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    assertThatThrownBy(() -> fridgeItemService.deleteItem(USER_ID, itemId))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(AppError.FRIDGE_ITEM_NOT_ACTIVE);
  }

  @Test
  void consumeItemDecrementsQuantityAndRecordsEvent() {
    UUID itemId = UUID.randomUUID();
    FridgeItem item = activeItem(itemId, new BigDecimal("3"));
    when(fridgeItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    FridgeItemResponse response =
        fridgeItemService.consumeItem(
            USER_ID, itemId, new FridgeItemConsumeRequest(new BigDecimal("1"), null));

    assertThat(response.quantity()).isEqualByComparingTo("2");
    assertThat(response.status()).isEqualTo(FridgeItemStatus.ACTIVE);
    verify(consumptionEventRepository).save(any(ConsumptionEvent.class));
  }

  @Test
  void consumeItemMarksItemConsumedWhenQuantityReachesZero() {
    UUID itemId = UUID.randomUUID();
    FridgeItem item = activeItem(itemId, new BigDecimal("2"));
    when(fridgeItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    FridgeItemResponse response =
        fridgeItemService.consumeItem(
            USER_ID, itemId, new FridgeItemConsumeRequest(new BigDecimal("2"), null));

    assertThat(response.quantity()).isEqualByComparingTo("0");
    assertThat(response.status()).isEqualTo(FridgeItemStatus.CONSUMED);
  }

  @Test
  void consumeItemThrowsWhenQuantityExceedsRemaining() {
    UUID itemId = UUID.randomUUID();
    FridgeItem item = activeItem(itemId, new BigDecimal("1"));
    when(fridgeItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    assertThatThrownBy(
            () ->
                fridgeItemService.consumeItem(
                    USER_ID, itemId, new FridgeItemConsumeRequest(new BigDecimal("2"), null)))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(AppError.INSUFFICIENT_QUANTITY);

    assertThat(item.getQuantity()).isEqualByComparingTo("1");
    verify(consumptionEventRepository, never()).save(any());
  }

  @Test
  void consumeItemRecordsGivenReason() {
    UUID itemId = UUID.randomUUID();
    FridgeItem item = activeItem(itemId, new BigDecimal("1"));
    when(fridgeItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    fridgeItemService.consumeItem(
        USER_ID,
        itemId,
        new FridgeItemConsumeRequest(new BigDecimal("1"), ConsumptionReason.EXPIRED));

    verify(consumptionEventRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                event -> event.getReason() == ConsumptionReason.EXPIRED));
  }

  @Test
  void consumeItemThrowsWhenUserIsNotMemberOfTheItemsGroup() {
    UUID itemId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();
    when(fridgeItemRepository.findById(itemId))
        .thenReturn(Optional.of(activeItem(itemId, new BigDecimal("1"))));
    when(groupMemberRepository.existsMember(GROUP_ID, outsiderId)).thenReturn(false);

    assertThatThrownBy(
            () ->
                fridgeItemService.consumeItem(
                    outsiderId, itemId, new FridgeItemConsumeRequest(new BigDecimal("1"), null)))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.NOT_GROUP_MEMBER);

    verify(consumptionEventRepository, never()).save(any());
    verify(fridgeItemRepository, never()).save(any());
  }

  private FridgeItem activeItem(UUID itemId, BigDecimal quantity) {
    FridgeItem item =
        new FridgeItem(
            GROUP_ID,
            null,
            "玉ねぎ",
            quantity,
            "個",
            FridgeItemCategory.VEGETABLE,
            null,
            LocalDate.now(),
            USER_ID,
            null);
    ReflectionTestUtils.setField(item, "id", itemId);
    return item;
  }
}
