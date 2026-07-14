package com.example.fridgeapp.auth;

import com.example.fridgeapp.common.AppError;
import com.example.fridgeapp.common.AppProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.springframework.stereotype.Service;

/** Google ID トークンを Google の公開鍵で検証する。audience（このアプリのクライアント ID）の一致も検証する。 */
@Service
public class GoogleIdTokenVerifierService {

  private final GoogleIdTokenVerifier verifier;

  public GoogleIdTokenVerifierService(AppProperties props) {
    this.verifier =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(List.of(props.google().clientId()))
            .build();
  }

  /**
   * ID トークンを検証し、ペイロード（sub・name・picture 等）を返す。
   *
   * @throws AuthException 署名・期限・audience の検証に失敗した場合や、Google への通信に失敗した場合（{@link
   *     AppError#INVALID_GOOGLE_TOKEN}）
   */
  public Payload verify(String idTokenString) {
    try {
      GoogleIdToken idToken = verifier.verify(idTokenString);
      if (idToken == null) {
        throw new AuthException(AppError.INVALID_GOOGLE_TOKEN);
      }
      return idToken.getPayload();
    } catch (GeneralSecurityException | IOException e) {
      throw new AuthException(AppError.INVALID_GOOGLE_TOKEN);
    }
  }
}
