package com.ticketon.ticketon.common.controller;

import com.ticketon.ticketon.common.constants.Urls;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttribute {

    @ModelAttribute("urls")
    public Class<?> urls() {
        return Urls.class;
    }

}
