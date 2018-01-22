package by.potato.helper;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Optional;

public class Geocoding {

    private static final GeoApiContext contextGoogle = new GeoApiContext.Builder().apiKey("AIzaSyAnSe8k3kMruEIhx8qDO40O2aXLloHwq9s").build();
    private static final Logger logger = LogManager.getLogger(Geocoding.class.getSimpleName());

    public static Optional<LatLng> getCoordFromAddress(String address) {

        logger.info("Geocoding address : " +address);

        try {
            GeocodingResult[] coordinates = GeocodingApi.newRequest(contextGoogle)
                    .address(address).language("ru").await();

            Double lat = coordinates[0].geometry.location.lat;
            Double lng = coordinates[0].geometry.location.lng;

            return Optional.of(new LatLng(lat, lng));

        } catch (ApiException | InterruptedException | IOException | ArrayIndexOutOfBoundsException e) {
            logger.error("Not coordinat for this address " + address);
            return Optional.empty();
        }
    }

}
