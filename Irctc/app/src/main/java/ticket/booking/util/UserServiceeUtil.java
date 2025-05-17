package ticket.booking.util;

import org.mindrot.jbcrypt.BCrypt;

public class UserServiceeUtil
{
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        // when someone enters plain text password this converts that to hashes by applying some salting (###$$)
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}