package com.example.fridgeapp.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.foodmaster.FoodMasterRepository;
import com.example.fridgeapp.fridge.FridgeItem;
import com.example.fridgeapp.fridge.FridgeItemException;
import com.example.fridgeapp.fridge.FridgeItemRepository;
import com.example.fridgeapp.fridge.FridgeItemResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShoppingItemServiceTest {

  private static final UUID GROUP_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private ShoppingItemRepository shoppingItemRepository;
  @Mock private FridgeItemRepository fridgeItemRepository;
  @Mock private FoodMasterRepository foodMasterRepository;
  @Mock private GroupMemberRepository groupMemberRepository;

  private ShoppingItemService shoppingItemService;

  @BeforeEach
  void setUp() {
    shoppingItemService =
        new ShoppingItemService(
            shoppingItemRepository,
            fridgeItemRepository,
            foodMasterRepository,
            new GroupAccessGuard(groupMemberRepository));
    lenient().when(groupMemberRepository.existsMember(GROUP_ID, USER_ID)).thenReturn(true);
  }

  @Test
  void listItemsThrowsWhenUserIsNotGroupMember() {
    UUID outsiderId = UUID.randomUUID();
    when(groupMemberRepository.existsMember(GROUP_ID, outsiderId)).thenReturn(false);

    assertThatThrownBy(() -> shoppingItemService.listItems(outsiderId, GROUP_ID))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.NOT_GROUP_MEMBER);
  }

  @Test
  void listItemsReturnsItemsInRegistrationOrder() {
    when(shoppingItemRepository.findByGroupIdOrderByCreatedAtAsc(GROUP_ID)).thenReturn(List.of());

    List<ShoppingItemResponse> result = shoppingItemService.listItems(USER_ID, GROUP_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void createItemStoresUncheckedItem() {
    when(shoppingItemRepository.save(any(ShoppingItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ShoppingItemCreateRequest request =
        new ShoppingItemCreateRequest(null, "牛乳", new BigDecimal("1"), "特売のとき");

    ShoppingItemResponse response = shoppingItemService.createItem(USER_ID, GROUP_ID, request);

    assertThat(response.displayName()).isEqualTo("牛乳");
    assertThat(response.memo()).isEqualTo("特売のとき");
    assertThat(response.checked()).isFalse();
  }

  @Test
  void createItemThrowsWhenFoodMasterDoesNotExist() {
    UUID foodMasterId = UUID.randomUUID();
    when(foodMasterRepository.existsById(foodMasterId)).thenReturn(false);

    ShoppingItemCreateRequest request =
        new ShoppingItemCreateRequest(foodMasterId, "牛乳", new BigDecimal("1"), null);

    assertThatThrownBy(() -> shoppingItemService.createItem(USER_ID, GROUP_ID, request))
        .isInstanceOf(ShoppingItemException.class)
        .extracting("error")
        .isEqualTo(AppError.FOOD_MASTER_NOT_FOUND);
  }

  @Test
  void updateItemLeavesOmittedFieldsUnchanged() {
    UUID itemId = UUID.randomUUID();
    ShoppingItem item = uncheckedItem(itemId);
    when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(shoppingItemRepository.save(item)).thenReturn(item);

    ShoppingItemUpdateRequest request = new ShoppingItemUpdateRequest(null, null, null, null, true);

    ShoppingItemResponse response = shoppingItemService.updateItem(USER_ID, itemId, request);

    assertThat(response.checked()).isTrue();
    assertThat(response.displayName()).isEqualTo("牛乳");
    assertThat(response.quantity()).isEqualByComparingTo("1");
  }

  @Test
  void updateItemThrowsWhenItemNotFound() {
    UUID itemId = UUID.randomUUID();
    when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.empty());

    ShoppingItemUpdateRequest request = new ShoppingItemUpdateRequest(null, null, null, null, true);

    assertThatThrownBy(() -> shoppingItemService.updateItem(USER_ID, itemId, request))
        .isInstanceOf(ShoppingItemException.class)
        .extracting("error")
        .isEqualTo(AppError.SHOPPING_ITEM_NOT_FOUND);
  }

  @Test
  void deleteItemRemovesItPhysically() {
    UUID itemId = UUID.randomUUID();
    ShoppingItem item = uncheckedItem(itemId);
    when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    shoppingItemService.deleteItem(USER_ID, itemId);

    verify(shoppingItemRepository).delete(item);
  }

  @Test
  void moveToFridgeThrowsWhenItemIsNotChecked() {
    UUID itemId = UUID.randomUUID();
    ShoppingItem item = uncheckedItem(itemId);
    when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    MoveToFridgeRequest request = new MoveToFridgeRequest("乳製品", null, null, null, null);

    assertThatThrownBy(() -> shoppingItemService.moveToFridge(USER_ID, itemId, request))
        .isInstanceOf(ShoppingItemException.class)
        .extracting("error")
        .isEqualTo(AppError.SHOPPING_ITEM_NOT_CHECKED);

    verify(fridgeItemRepository, never()).save(any());
    verify(shoppingItemRepository, never()).delete(any());
  }

  @Test
  void moveToFridgeThrowsOnUnknownCategory() {
    UUID itemId = UUID.randomUUID();
    ShoppingItem item = checkedItem(itemId);
    when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));

    MoveToFridgeRequest request = new MoveToFridgeRequest("宇宙食", null, null, null, null);

    assertThatThrownBy(() -> shoppingItemService.moveToFridge(USER_ID, itemId, request))
        .isInstanceOf(FridgeItemException.class)
        .extracting("error")
        .isEqualTo(AppError.INVALID_FRIDGE_ITEM_CATEGORY);

    verify(fridgeItemRepository, never()).save(any());
    verify(shoppingItemRepository, never()).delete(any());
  }

  @Test
  void moveToFridgeCreatesFridgeItemAndRemovesShoppingItem() {
    UUID itemId = UUID.randomUUID();
    ShoppingItem item = checkedItem(itemId);
    when(shoppingItemRepository.findById(itemId)).thenReturn(Optional.of(item));
    when(fridgeItemRepository.save(any(FridgeItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MoveToFridgeRequest request =
        new MoveToFridgeRequest("乳製品", LocalDate.of(2026, 8, 1), "本", null, null);

    FridgeItemResponse response = shoppingItemService.moveToFridge(USER_ID, itemId, request);

    assertThat(response.displayName()).isEqualTo("牛乳");
    assertThat(response.category()).isEqualTo("乳製品");
    assertThat(response.unit()).isEqualTo("本");
    assertThat(response.expiresAt()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(response.purchasedAt()).isEqualTo(LocalDate.now());
    assertThat(response.purchasedBy()).isEqualTo(USER_ID);

    ArgumentCaptor<FridgeItem> captor = ArgumentCaptor.forClass(FridgeItem.class);
    verify(fridgeItemRepository).save(captor.capture());
    assertThat(captor.getValue().getGroupId()).isEqualTo(GROUP_ID);

    verify(shoppingItemRepository).delete(item);
  }

  @Test
  void clearCheckedItemsThrowsWhenUserIsNotGroupMember() {
    UUID outsiderId = UUID.randomUUID();
    when(groupMemberRepository.existsMember(GROUP_ID, outsiderId)).thenReturn(false);

    assertThatThrownBy(() -> shoppingItemService.clearCheckedItems(outsiderId, GROUP_ID))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.NOT_GROUP_MEMBER);

    verify(shoppingItemRepository, never()).deleteCheckedByGroupId(any());
  }

  @Test
  void clearCheckedItemsDelegatesToRepository() {
    shoppingItemService.clearCheckedItems(USER_ID, GROUP_ID);

    verify(shoppingItemRepository).deleteCheckedByGroupId(GROUP_ID);
  }

  private ShoppingItem uncheckedItem(UUID itemId) {
    ShoppingItem item = new ShoppingItem(GROUP_ID, null, "牛乳", new BigDecimal("1"), null);
    ReflectionTestUtils.setField(item, "id", itemId);
    return item;
  }

  private ShoppingItem checkedItem(UUID itemId) {
    ShoppingItem item = uncheckedItem(itemId);
    item.update(null, null, null, null, true);
    return item;
  }
}
