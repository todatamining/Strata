package com.opengamma.strata.pricer.capfloor;

import java.time.ZonedDateTime;

import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;

public interface IborCapletFloorletVolatilityCalibrator<T> {

  
  public T calibrate(ZonedDateTime calibrationDateTime, RawOptionData data, RatesProvider ratesProvider);
  
}
