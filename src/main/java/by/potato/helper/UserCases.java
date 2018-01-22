package by.potato.helper;

import by.potato.holder.Department;
import com.google.maps.model.LatLng;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class UserCases {


    public List<Department> geoDepartmentLimitCount(String place, int count) {

   //     LatLng currentPosition = Geocoding.getLatLng(place);

        LatLng currentPosition = new LatLng();
        currentPosition.lat = 55.160694;
        currentPosition.lng = 30.206732;

     //   return DataBaseHelper.getInstance().geoDepartmentLimitCount(currentPosition,count);
        return  null;
    }


    public List<Department> geoDepartmentLimitDistCount(String place, double dist, int count) {

  //      LatLng currentPosition = Geocoding.getLatLng(place);
        LatLng currentPosition = new LatLng();
        currentPosition.lat = 55.160694;
        currentPosition.lng = 30.206732;

//        return DataBaseHelper.getInstance().geoDepartmentLimitDistCount(currentPosition, dist, count);
        return  null;
    }


    private static class LazyUserCases {
        public static UserCases userCases = new UserCases();
    }

    public static UserCases getInstance() {
        return LazyUserCases.userCases;
    }

}
