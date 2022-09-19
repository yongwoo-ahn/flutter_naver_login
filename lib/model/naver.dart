import 'package:flutter_naver_login/src/clock.dart';

enum NaverLoginStatus { loggedIn, cancelledByUser, error }

const Map<String, dynamic> accessResultError = {
  'status': 'error',
  'errorMessage': 'Login Failed',
  'accessToken': 'no token',
  'refreshToken': 'no refreshToken',
  'expiresAt': 'no token',
  'tokenType': 'no token',
};

class NaverLoginResult {
  final String status;
  final NaverAccountResult account;
  final String errorMessage;
  final NaverAccessToken accessToken;

  NaverLoginResult.fromMap(Map<String, dynamic> map)
      : status = _parseStatus(map['status'] ?? ''),
        accessToken = NaverAccessToken.fromMap(map),
        errorMessage = map['errorMessage'] ?? '',
        account = new NaverAccountResult.fromMap(map);

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

  NaverAccessToken.fromMap(Map<String, dynamic> map)
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

  NaverAccountResult.fromMap(Map<String, dynamic> map)
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
