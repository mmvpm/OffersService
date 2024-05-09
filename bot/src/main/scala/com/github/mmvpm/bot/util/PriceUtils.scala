package com.github.mmvpm.bot.util

object PriceUtils {

  def priceText(price: Int): String =
    price match {
      case 0 => "Бесплатно"
      case _ =>
        val suffix = price % 10 match {
          case 1         => "рубль"
          case 2 | 3 | 4 => "рубля"
          case _         => "рублей"
        }
        s"$price $suffix"
    }
}
