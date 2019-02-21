package oriontech.com.weathernow.views.stars.model

import oriontech.com.weathernow.views.stars.model.stars.BaseStar
import oriontech.com.weathernow.views.stars.model.stars.RoundyStar
import oriontech.com.weathernow.views.stars.model.stars.ShinyStar
import oriontech.com.weathernow.views.stars.model.stars.TinyStar

object InterstellarFactory {

    fun create(starConstraints: StarConstraints, x: Int, y: Int, color: Int, listener: BaseStar.StarCompleteListener): Star {
        val starSize = starConstraints.getRandomStarSize()

        return if (starSize >= starConstraints.bigStarThreshold) {

            // big star ones randomly be Shiny or Round
            if (Math.random() < 0.7) {
                ShinyStar(starConstraints, x, y, color, listener)
            } else {
                RoundyStar(starConstraints, x, y, color, listener)
            }
        } else {
            // others will ne tiny
            TinyStar(starConstraints, x, y, color, listener)
        }
    }

}