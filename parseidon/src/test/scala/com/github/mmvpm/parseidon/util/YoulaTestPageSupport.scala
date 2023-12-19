package com.github.mmvpm.parseidon.util

import com.github.mmvpm.parseidon.model.{YoulaItem, YoulaOffer, YoulaUser}
import com.github.mmvpm.util.StringUtils.RichString

import scala.io.Source
import scala.util.Using

trait YoulaTestPageSupport {

  val youlaTestPage: String =
    Using(Source.fromResource("youla-page.txt"))(_.getLines().mkString("\n")).get

  val youlaTestPageItem: YoulaItem =
    YoulaItem(
      YoulaOffer(
        name = "Небольшая собака в добрые руки",
        price = 0,
        text = "Санкт-Петербург",
        photoUrls = List(
          "https://cdn0.youla.io/files/images/orig/64/1a/641a1ef284e6b960e65be7bd-1.jpg",
          "https://cdn0.youla.io/files/images/orig/64/1a/641a1ef629cf222424020b8c-1.jpg",
          "https://cdn0.youla.io/files/images/orig/64/1a/641a1ef9bed21b46be4153dc-1.jpg",
          "https://cdn0.youla.io/files/images/orig/64/1a/641a1efc29e3b42d9c4e0a2f-1.jpg",
          "https://cdn0.youla.io/files/images/orig/64/1a/641a1eff069516788332f77d-1.jpg"
        ).map(_.toURL)
      ),
      YoulaUser(id = "59c2ea23aaab28926774ad23", name = "Анастасия Х.")
    )
}
