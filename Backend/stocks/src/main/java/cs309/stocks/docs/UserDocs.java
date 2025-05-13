package cs309.stocks.docs;

import cs309.stocks.Users.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDocs {
    private String username;
    private String password;
    private UserRole role;
    private String email;
}
