import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_naver_login_platform_interface.dart';
import 'model/naver.dart';

/// An implementation of [FlutterNaverLoginPlatform] that uses method channels.
class MethodChannelFlutterNaverLogin extends FlutterNaverLoginPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_naver_login');

  @override
  Future<NaverLoginResult> logIn() async {
    final Map<dynamic, dynamic> res = await methodChannel.invokeMethod('logIn');

    return new NaverLoginResult.fromMap(castStringMap(res));
  }
  @override
  Future<NaverLoginResult> logOut() async {
    final Map<dynamic, dynamic> res = await methodChannel.invokeMethod('logOut');

    return new NaverLoginResult.fromMap(castStringMap(res));
  }
  @override
  Future<NaverLoginResult> logOutAndDeleteToken() async {
    final Map<dynamic, dynamic> res =
    await methodChannel.invokeMethod('logoutAndDeleteToken');

    return new NaverLoginResult.fromMap(castStringMap(res));
  }
  @override
  Future<bool> get isLoggedIn async {
    if ((await currentAccessToken).isValid())
      return true;
    else
      return false;
  }
  @override
  Future<NaverAccountResult> currentAccount() async {
    final Map<dynamic, dynamic> res =
    await methodChannel.invokeMethod('getCurrentAcount');

    return new NaverAccountResult.fromMap(castStringMap(res));
  }
  @override
  Future<NaverAccessToken> get currentAccessToken async {
    final Map<dynamic, dynamic>? accessToken =
    await methodChannel.invokeMethod('getCurrentAccessToken');

    if (accessToken == null)
      return NaverAccessToken.fromMap(accessResultError);
    else
      return new NaverAccessToken.fromMap(
          castStringMap(accessToken));
  }
  @override
  Future<NaverAccessToken> refreshAccessTokenWithRefreshToken() async {
    final accessToken = await currentAccessToken;
    if (accessToken.refreshToken.isNotEmpty ||
        accessToken.refreshToken != 'no token') {
      await methodChannel.invokeMethod('refreshAccessTokenWithRefreshToken');
    }
    return (await currentAccessToken);
  }

  static Map<String, dynamic> castStringMap(Map<dynamic, dynamic> map) {
    try {
      return map.cast<String, dynamic>();
    } catch (e) {
      return accessResultError;
    }
  }
}
