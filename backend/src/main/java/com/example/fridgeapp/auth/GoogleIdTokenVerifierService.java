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

@Service
public class GoogleIdTokenVerifierService {

  private final GoogleIdTokenVerifier verifier;

  public GoogleIdTokenVerifierService(AppProperties props) {
    this.verifier =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(List.of(props.google().clientId()))
            .build();
  }

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
