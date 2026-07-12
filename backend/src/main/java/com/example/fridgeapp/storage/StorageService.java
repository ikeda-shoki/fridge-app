package com.example.fridgeapp.storage;

/** dev: ローカル FS 実装、本番: S3 / Supabase Storage への切り替えを想定した画像ストレージ抽象化。 */
public interface StorageService {

  /** コンテンツを保存し、{@link #delete(String)} に渡せる不透明なパス文字列を返す。 */
  String store(byte[] content, String extension);

  void delete(String path);
}
