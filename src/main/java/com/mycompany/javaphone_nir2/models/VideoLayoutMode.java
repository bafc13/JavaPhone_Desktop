/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javaphone_nir2.models;

/**
 *
 * @author bafc13
 */
/**
 * Режимы отображения видео-панелей в звонке
 */
public enum VideoLayoutMode {
    PIP_PRIMARY_FIRST, // Большая: камера 1, маленькая в углу: камера 2
    PIP_PRIMARY_SECOND, // Большая: камера 2, маленькая в углу: камера 1
    SPLIT                // Обе камеры одинакового размера
}
