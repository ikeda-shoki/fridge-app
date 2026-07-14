package com.example.fridgeapp.storage;

/** dev: ローカル FS 実装、本番: S3 / Supabase Storage への切り替えを想定した画像ストレージ抽象化。 */
public interface StorageService {

  /**
   * コンテンツを保存し、{@link #delete(String)} に渡せる不透明なパス文字列を返す。
   *
   * @throws StorageException 保存に失敗した場合
   */
  String store(byte[] content, String extension);

  /**
   * 保存済みのコンテンツを削除する。既に存在しない場合も成功として扱う。
   *
   * @param path {@link #store(byte[], String)} が返したパス文字列
   * @throws StorageException 削除に失敗した場合、パスが不正な場合
   */
  void delete(String path);
}
