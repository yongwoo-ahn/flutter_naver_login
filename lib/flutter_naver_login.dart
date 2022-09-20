import 'dart:async';
import 'dart:core';

import 'flutter_naver_login_platform_interface.dart';
import 'model/naver.dart';

class FlutterNaverLogin {
  Future<NaverLoginResult> logIn() async {
    return FlutterNaverLoginPlatform.instance.logIn();
  }

  Future<NaverLoginResult> logOut() async {
    return FlutterNaverLoginPlatform.instance.logOut();
  }

  Future<NaverLoginResult> logOutAndDeleteToken() async {
    return FlutterNaverLoginPlatform.instance.logOutAndDeleteToken();
  }

  Future<bool> get isLoggedIn async {
    return FlutterNaverLoginPlatform.instance.isLoggedIn;
  }

  Future<NaverAccountResult> currentAccount() async {
    return FlutterNaverLoginPlatform.instance.currentAccount();
  }

  Future<NaverAccessToken> get currentAccessToken async {
    return FlutterNaverLoginPlatform.instance.currentAccessToken;
  }

  Future<NaverAccessToken> refreshAccessTokenWithRefreshToken() async {
    return FlutterNaverLoginPlatform.instance
        .refreshAccessTokenWithRefreshToken();
  }
}
