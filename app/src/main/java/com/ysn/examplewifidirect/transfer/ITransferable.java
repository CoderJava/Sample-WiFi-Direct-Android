/*
 * Created by YSN Studio on 5/23/18 2:52 PM
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 5/23/18 2:52 PM
 */

package com.ysn.examplewifidirect.transfer;

import java.io.Serializable;

public interface ITransferable extends Serializable {

    int getRequestCode();

    String getRequestType();

    String getData();

}
