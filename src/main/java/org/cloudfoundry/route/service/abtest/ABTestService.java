package org.cloudfoundry.route.service.abtest;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;

@Component
public class ABTestService {
	private final static Logger logger = LoggerFactory.getLogger(ABTestService.class);

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static int baseADomainPercentage, baseBDomainPercentage;

	private static int aDomainCurrentPercentage, bDomainCurrentPercentage;

	private static float baseARatio, baseBRatio;

	private static Random random = new Random();

	@Value("${route.service.abtest.domainAPercentage}")
	public void setDomainARatio(String value) {
		baseADomainPercentage = Integer.parseInt(value);
	}

	@Value("${route.service.abtest.domainBPercentage}")
	public void setDomainBRatio(String value) {
		baseBDomainPercentage = Integer.parseInt(value);
	}

	static {
		float totalPercentage = baseADomainPercentage + baseBDomainPercentage;

		baseARatio = aDomainCurrentPercentage / totalPercentage;
		baseBRatio = bDomainCurrentPercentage / totalPercentage;
	}

	public boolean isExistingSession(RequestEntity<?> incoming) {

		List<String> cookies = incoming.getHeaders().get(Controller.COOKIE);
		if (cookies != null) {
			for (String cookieItem : cookies) {
				for (String cookie : cookieItem.split(";")) {
					String[] keyValue = cookie.split("=");
					if (Controller.JSESSIONID.endsWith(keyValue[0].trim())) {
						String trackingId = keyValue[1];
						String value = (String) redisTemplate.opsForValue().get(trackingId);
						redisTemplate.opsForValue().set(trackingId, "", 10, TimeUnit.MINUTES);
						return value != null;
					}
				}
			}
		}

		logger.error(String.format("No %s value present in %s header", Controller.JSESSIONID, Controller.COOKIE));
		return false;
	}

	public synchronized static boolean isDomainA() {
		float aRatio, bRatio;

		if (aDomainCurrentPercentage == 0 && bDomainCurrentPercentage == 0) {
			aDomainCurrentPercentage = baseADomainPercentage;
			bDomainCurrentPercentage = baseBDomainPercentage;
		}

		float newTotalRatio = aDomainCurrentPercentage + bDomainCurrentPercentage;
		aRatio = aDomainCurrentPercentage / newTotalRatio;
		bRatio = bDomainCurrentPercentage / newTotalRatio;

		if (aRatio > baseARatio && bRatio < baseBRatio) { 			// Domain A
			--aDomainCurrentPercentage;
			return true;
		} else if (aRatio < baseARatio && bRatio > baseBRatio) { 	// Domain B
			--bDomainCurrentPercentage;
			return false;
		} else if (random.nextBoolean()) { 							// Domain A
			--aDomainCurrentPercentage;
			return true;
		} else { 													// Domain B
			--bDomainCurrentPercentage;
			return false;
		}
	}
}
