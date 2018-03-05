package by.potato.holder;

public class SQL {
    public static final String UpdateCities = "INSERT INTO Cities VALUES (null,?)";
    public static final String GetAddressFromCity = "select d.address from Departments as d, Cities as c where c.id = d.id_cities and c.name = ?";
    public static final String UpdateWorkTime = "UPDATE Departments SET workTimes = ?, workTimesOriginal = ? WHERE link_work_time = ?";
    public static final String DeleteUnusedDepartment = "DELETE FROM Departments WHERE last_update < ?";
    public static final String InsertQuestion = "INSERT INTO Questions (id, chart_id, message) VALUES (NULL,?,?)";
    public static final String GetIDCities = "select * from Cities";
    public static final String GetLinkWorkTime = "select link_work_time from Departments";
    public static final String GetUserSettings = "select rub_sell,rub_buy,usd_sell,usd_buy,eur_sell,eur_buy, phone, work_time from UserSettings where chat_id = ?";

    public static final String GetDepartmentByBankAndCity = "SELECT " +
            "dep.address," +
            "dep.euro_buy," +
            "dep.euro_sell," +
            "dep.euro_multiplier," +
            "dep.rub_buy," +
            "dep.rub_sell," +
            "dep.rub_multiplier," +
            "dep.doll_buy," +
            "dep.doll_sell," +
            "dep.doll_multiplier," +
            "dep.phone," +
            "dep.workTimesOriginal," +
            "dep.lat," +
            "dep.lng " +
            " FROM Departments as dep, Cities as c  WHERE c.name = ? and c.id = dep.id_cities and bank_name = ?";

    public static final String GetBanksByCity = "SELECT DISTINCT bank_name " +
            "FROM Departments as dep, Cities as c  " +
            "WHERE c.name = ? and c.id = dep.id_cities " +
            "ORDER BY bank_name ASC";

    public static final String UpdateDepartments = "INSERT INTO Departments " +
            "(address,bank_name,id_cities,doll_buy,doll_sell,doll_multiplier,euro_buy,euro_sell,euro_multiplier,rub_buy,rub_sell,rub_multiplier,lat,lng,workTimes,link_work_time,phone,last_update)" +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)  " +
            "ON DUPLICATE KEY UPDATE " +
            "doll_buy = ?," +
            "doll_sell = ?," +
            "doll_multiplier = ?," +
            "euro_buy = ?," +
            "euro_sell = ?," +
            "euro_multiplier = ?," +
            "rub_buy = ?," +
            "rub_sell = ?," +
            "rub_multiplier = ?," +
            "last_update = ?";

    public static final String GetCoursesByCity = "SELECT dep.bank_name, " +
            "dep.address," +
            "dep.euro_buy," +
            "dep.euro_sell," +
            "dep.euro_multiplier," +
            "dep.rub_buy," +
            "dep.rub_sell," +
            "dep.rub_multiplier," +
            "dep.doll_buy," +
            "dep.doll_sell," +
            "dep.doll_multiplier," +
            "dep.phone," +
            "dep.workTimesOriginal," +
            "dep.lat," +
            "dep.lng" +
            " FROM Departments as dep, Cities as c  WHERE c.name = ? and c.id = dep.id_cities";

    public static final String UpdateUserSettings = "INSERT INTO UserSettings " +
            "(chat_id,rub_sell,rub_buy,usd_sell,usd_buy,eur_sell,eur_buy,phone,work_time)" +
            "VALUES(?,?,?,?,?,?,?,?,?)  " +
            "ON DUPLICATE KEY UPDATE " +
            "rub_sell = ?," +
            "rub_buy = ?," +
            "usd_sell = ?," +
            "usd_buy = ?," +
            "eur_sell = ?," +
            "eur_buy = ?," +
            "phone = ?," +
            "work_time = ?";
}
