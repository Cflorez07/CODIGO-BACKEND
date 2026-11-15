package com.app.priorix.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.priorix.Service.PredictorTriajeService;

@Controller
@RequestMapping({"/admin/ia", "/admin/modelo-predictivo"})
public class AdminIaController {

    @Autowired
    private PredictorTriajeService predictorTriajeService;

    @GetMapping
    public String verSupervisionIa(Model model) {
        Map<String, Object> metricas = predictorTriajeService.obtenerMetricasDashboard();
        model.addAllAttributes(metricas);
        return "admin/ia";
    }
}
