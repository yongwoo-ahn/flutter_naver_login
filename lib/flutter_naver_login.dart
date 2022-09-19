import 'dart:async';

import 'package:flutter/services.dart';

import 'src/clock.dart';

class FlutterNaverLogin {
  static const MethodChannel _channel =
      const MethodChannel('flutter_naver_login');

  static const Map<String, dynamic> accessResultError = {
    'status': 'error',
    'errorMessage': 'Login Failed',
    'accessToken': 'no token',
    'refreshToken': 'no refreshToken',
    'expiresAt': 'no token',
    'tokenType': 'no token',
  };

  static Future<void> initializeSDK() async {
    await _channel.invokeMethod('initializeSDK');
    return;
  }

  static Future<NaverLoginResult> logIn() async {
    final Map<dynamic, dynamic> res = await _channel.invokeMethod('logIn');

    return new NaverLoginResult._(FlutterNaverLogin.castStringMap(res));
  }

  static Future<NaverLoginResult> logOut() async {
    final Map<dynamic, dynamic> res = await _channel.invokeMethod('logOut');

    return new NaverLoginResult._(FlutterNaverLogin.castStringMap(res));
  }

  static Future<NaverLoginResult> logOutAndDeleteToken() async {
    final Map<dynamic, dynamic> res =
        await _channel.invokeMethod('logoutAndDeleteToken');

    return new NaverLoginResult._(FlutterNaverLogin.castStringMap(res));
  }

  static Future<bool> get isLoggedIn async {
    if ((await currentAccessToken).isValid())
      return true;
    else
      return false;
  }

  static Future<NaverAccountResult> currentAccount() async {
    final Map<dynamic, dynamic> res =
        await _channel.invokeMethod('getCurrentAcount');

    return new NaverAccountResult._(FlutterNaverLogin.castStringMap(res));
  }

  static Future<NaverAccessToken> get currentAccessToken async {
    final Map<dynamic, dynamic>? accessToken =
        await _channel.invokeMethod('getCurrentAccessToken');

    if (accessToken == null)
      return NaverAccessToken._(FlutterNaverLogin.accessResultError);
    else
      return new NaverAccessToken._(
          FlutterNaverLogin.castStringMap(accessToken));
  }

  static Future<NaverAccessToken> refreshAccessTokenWithRefreshToken() async {
    final accessToken = await currentAccessToken;
    if (accessToken.refreshToken.isNotEmpty ||
        accessToken.refreshToken != 'no token') {
      await _channel.invokeMethod('refreshAccessTokenWithRefreshToken');
    }
    return (await currentAccessToken);
  }

  static Map<String, dynamic> castStringMap(Map<dynamic, dynamic> map) {
    try {
      return map.cast<String, dynamic>();
    } catch (e) {
      return FlutterNaverLogin.accessResultError;
    }
  }
}

enum NaverLoginStatus { loggedIn, cancelledByUser, error }

class NaverLoginResult {
  final String status;
  final NaverAccountResult account;
  final String errorMessage;
  final NaverAccessToken accessToken;

  NaverLoginResult._(Map<String, dynamic> map)
      : status = _parseStatus(map['status'] ?? ''),
        accessToken = NaverAccessToken._(map),
        errorMessage = map['errorMessage'] ?? '',
        account = new NaverAccountResult._(map);

  static String _parseStatus(String status) {
    switch (status) {
      case 'loggedIn':
        return NaverLoginStatus.loggedIn.name;
      case 'cancelledByUser':
        return NaverLoginStatus.cancelledByUser.name;
      case 'error':
        return NaverLoginStatus.error.name;
    }
    throw new StateError('Invalid status: $status');
  }

  @override
  String toString() =>
      '{ status: $status, account: $account, errorMessage: $errorMessage, accessToken: $accessToken }';
}

class NaverAccessToken {
  final String accessToken;
  final String refreshToken;
  final String expiresAt;
  final String tokenType;

  bool isValid() {
    if (expiresAt.isEmpty || expiresAt == 'no token') return false;
    bool timeValid = Clock.now().isBefore(DateTime.parse(expiresAt));
    bool tokenExist = accessToken.isNotEmpty && accessToken != 'no token';
    return timeValid && tokenExist;
  }

  NaverAccessToken._(Map<String, dynamic> map)
      : accessToken = map['accessToken'] ?? '',
        refreshToken = map['refreshToken'] ?? '',
        expiresAt = map['expiresAt'] ?? '',
        tokenType = map['tokenType'] ?? '';

  @override
  String toString() =>
      '{ accessToken: $accessToken, refreshToken: $refreshToken, expiresAt: $expiresAt, tokenType: $tokenType }';
}

class NaverAccountResult {
  final String nickname;
  final String id;
  final String name;
  final String email;
  final String gender;
  final String age;
  final String birthday;
  final String birthyear;
  final String profileImage;
  final String mobile;
  final String mobileE164;

  NaverAccountResult._(Map<String, dynamic> map)
      : nickname = map['nickname'] ?? '',
        id = map['id'] ?? '',
        name = map['name'] ?? '',
        email = map['email'] ?? '',
        gender = map['gender'] ?? '',
        age = map['age'] ?? '',
        birthday = map['birthday'] ?? '',
        birthyear = map['birthyear'] ?? '',
        profileImage = map['profile_image'] ?? '',
        mobile = map['mobile'] ?? '',
        mobileE164 = map['mobileE164'] ?? '';

  @override
  String toString() {
    return '{ '
        'nickname: $nickname, '
        'id: $id, '
        'name: $name, '
        'email: $email, '
        'gender: $gender, '
        'age: $age, '
        'birthday: $birthday, '
        'birthyear: $birthyear, '
        'profileImage: $profileImage, '
        'mobile: $mobile, '
        'mobileE164: $mobileE164'
        ' }';
  }
}
