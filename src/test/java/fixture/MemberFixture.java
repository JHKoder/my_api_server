package fixture;

import com.example.my_api_server.entity.Member;

public class MemberFixture {

    //이메일, 비밀번호 (이메일은 고정된 값을 쓴다고 가정)
    public static Member.MemberBuilder defaultMember() {
        return Member.builder()
                .email("test1@gmail.com");
    }
}
