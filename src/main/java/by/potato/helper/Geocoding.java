package by.potato.helper;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.yandex.gecoder.client.GeoObject;
import ru.yandex.gecoder.client.GeocoderResponse;
import ru.yandex.gecoder.client.YaGeocoder;

import java.io.IOException;
import java.util.Optional;

public class Geocoding {

    private static final GeoApiContext contextGoogle = new GeoApiContext.Builder().apiKey("AIzaSyAnSe8k3kMruEIhx8qDO40O2aXLloHwq9s").build();
    private static final Logger logger = LogManager.getLogger(Geocoding.class.getSimpleName());

    public static Optional<LatLng> getCoordFromAddressCommon(String address) {
        logger.info("Geocoding address : " + address);

        Optional<LatLng> location = getCoordFromAddressGoogle(address);

        if (!location.isPresent()) {
            location = getCoordFromAddressYandex(address);
        }

        return location;
    }

    private static Optional<LatLng> getCoordFromAddressGoogle(String address) {

        logger.info("Google geocoding.");

        try {
            GeocodingResult[] coordinates = GeocodingApi.newRequest(contextGoogle)
                    .address(address).language("ru").await();

            Double lat = coordinates[0].geometry.location.lat;
            Double lng = coordinates[0].geometry.location.lng;

            return Optional.of(new LatLng(lat, lng));

        } catch (ApiException | InterruptedException | IOException | ArrayIndexOutOfBoundsException e) {
            logger.error("  Google. Not coordinat for this address " + address);
            return Optional.empty();
        }
    }

    private static Optional<LatLng> getCoordFromAddressYandex(String address) {

        logger.info("Yandex geocoding.");

        YaGeocoder geocoder = new YaGeocoder(new DefaultHttpClient());

        try {
            GeocoderResponse response = geocoder.directGeocode(address);

            GeoObject geoObject = response.getGeoObjects().get(0);//первый результат самый точный

            Double lat = geoObject.getPoint().getLat();
            Double lng = geoObject.getPoint().getLon();

            return Optional.of(new LatLng(lat, lng));

        } catch (IOException | IndexOutOfBoundsException e) {
            logger.error("Yandex. Not coordinat for this address " + address);
            return Optional.empty();

        }
    }

}
