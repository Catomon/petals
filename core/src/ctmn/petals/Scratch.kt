package ctmn.petals

//                    game.screen = PlayScreenTemplate.test2Players(game, JsonLevel.fromFile(testMapName).initActors(assets)).apply {
//                        playStage.clearGameActors()
//
//                        MapGenerator().generate(playStage)
//
//                        queueAddUnitAction(FairySword().player(localPlayer).position(5, 5))
//                        queueAddUnitAction(FairyPike().position(5, 5))
//                        queueAddUnitAction(FairySword().position(5, 5))
//
//                        playStage.addAction(UpdateAction {
//                            playStage.actors.forEach {
//                                if (it is SmoothMoveEffect)
//                                    return@UpdateAction false
//                            }
//
//                            playStage.addActor(SmoothMoveEffect(100f, 1.5f))
//
//                            false
//                        })
//                    }