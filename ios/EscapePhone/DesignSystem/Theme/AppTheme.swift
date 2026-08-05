import SwiftUI

enum AppTheme {
    static let background = Color(red: 0.025, green: 0.045, blue: 0.09)
    static let card = Color(red: 0.10, green: 0.14, blue: 0.19)
    static let accent = Color(red: 0.22, green: 0.88, blue: 0.72)
    static let warning = Color(red: 1.0, green: 0.55, blue: 0.35)
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.headline).frame(maxWidth: .infinity).padding().background(AppTheme.accent.opacity(configuration.isPressed ? 0.65 : 1)).foregroundStyle(.black).clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct CardModifier: ViewModifier {
    func body(content: Content) -> some View { content.padding().background(AppTheme.card).clipShape(RoundedRectangle(cornerRadius: 18)).overlay(RoundedRectangle(cornerRadius: 18).stroke(.white.opacity(0.08))) }
}
extension View { func gameCard() -> some View { modifier(CardModifier()) } }

struct DragReorderHandle: View {
    let itemId: String
    let currentIndex: Int
    let itemCount: Int
    let enabled: Bool
    let onStep: (Int) -> Void
    @State private var lastStep = 0

    var body: some View {
        VStack(spacing: 5) {
            Capsule().frame(width: 28, height: 4)
            Capsule().frame(width: 20, height: 4)
            Capsule().frame(width: 12, height: 4)
        }
        .foregroundStyle(enabled ? AppTheme.accent : .secondary)
        .frame(width: 44, height: 52)
        .contentShape(Rectangle())
        .gesture(DragGesture(minimumDistance: 2).onChanged { value in
            guard enabled else { return }
            let step = Int(value.translation.height / 34)
            guard step != lastStep else { return }
            let direction = step > lastStep ? 1 : -1
            for _ in 0..<abs(step - lastStep) { onStep(direction) }
            lastStep = step
        }.onEnded { _ in lastStep = 0 })
        .accessibilityLabel("순서 이동 핸들")
        .accessibilityHint("위아래로 쓸어 항목 순서를 변경합니다")
        .accessibilityAdjustableAction { direction in
            if direction == .increment, currentIndex < itemCount - 1 { onStep(1) }
            if direction == .decrement, currentIndex > 0 { onStep(-1) }
        }
    }
}

struct ShakeEffect: GeometryEffect {
    var amount: CGFloat = 8
    var shakesPerUnit = 3
    var animatableData: CGFloat
    func effectValue(size: CGSize) -> ProjectionTransform { ProjectionTransform(CGAffineTransform(translationX: amount * sin(animatableData * .pi * CGFloat(shakesPerUnit)), y: 0)) }
}
