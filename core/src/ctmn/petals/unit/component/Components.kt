package ctmn.petals.unit.component

/** blacklist
 *   if (component is ViewComponent) continue
 *             if (component is MatchUpBonusComponent) continue
 *             if (component is TerrainPropComponent) continue
 *             if (component is ShopComponent) continue */
object Components {
    // these components are being copied to saved state
    val classes = arrayOf(
        UnitComponent::class.java,
        AbilitiesComponent::class.java,
        AttackComponent::class.java,
        BarrierComponent::class.java,
        BonusFieldComponent::class.java,
        BuffsComponent::class.java,
        FollowerComponent::class.java,
        InvisibilityComponent::class.java,
        TileEffectComponent::class.java,
        LeaderComponent::class.java,
        LevelComponent::class.java,
        RoamingPosition::class.java,
        SummonableComponent::class.java,
        SummonerComponent::class.java,
        TraitComponent::class.java,
        ReloadingComponent::class.java,
        MoveAfterAttackComponent::class.java,
        WaypointComponent::class.java,
    )
}