import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_naver_login_method_channel.dart';
import 'model/naver.dart';

abstract class FlutterNaverLoginPlatform extends PlatformInterface {
  /// Constructs a FlutterSocialLoginPlatform.
  FlutterNaverLoginPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterNaverLoginPlatform _instance = MethodChannelFlutterNaverLogin();

  /// The default instance of [FlutterNaverLoginPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterNaverLogin].
  static FlutterNaverLoginPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterNaverLoginPlatform] when
  /// they register themselves.
  static set instance(FlutterNaverLoginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> initializeSDK() async {
    throw UnimplementedError('initializeSDK() has not been implemented.');
  }

  Future<NaverLoginResult> logIn() async {
    throw UnimplementedError('logIn() has not been implemented.');
  }

  Future<NaverLoginResult> logOut() async {
    throw UnimplementedError('logOut() has not been implemented.');
  }

  Future<NaverLoginResult> logOutAndDeleteToken() async {
    throw UnimplementedError('logOutAndDeleteToken() has not been implemented.');
  }

  Future<bool> get isLoggedIn async {
    throw UnimplementedError('isLoggedIn() has not been implemented.');
  }

  Future<NaverAccountResult> currentAccount() async {
    throw UnimplementedError('currentAccount() has not been implemented.');
  }

  Future<NaverAccessToken> get currentAccessToken async {
    throw UnimplementedError('currentAccessToken() has not been implemented.');
  }

  Future<NaverAccessToken> refreshAccessTokenWithRefreshToken() async {
    throw UnimplementedError('refreshAccessTokenWithRefreshToken() has not been implemented.');
  }
}
