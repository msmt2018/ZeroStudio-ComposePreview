# classes6.dex

.class public final Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;
.super Ljava/lang/Object;
.source "BaseSingleButtonAndTextInputSingleBinding.java"

# interfaces
.implements Landroidx/viewbinding/ViewBinding;


# instance fields
.field public final baseTextInputView:Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;

.field public final button:Lcom/google/android/material/button/MaterialButton;

.field public final coordinator:Landroidx/coordinatorlayout/widget/CoordinatorLayout;

.field private final rootView:Landroidx/coordinatorlayout/widget/CoordinatorLayout;


# direct methods
.method private constructor <init>(Landroidx/coordinatorlayout/widget/CoordinatorLayout;Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;Lcom/google/android/material/button/MaterialButton;Landroidx/coordinatorlayout/widget/CoordinatorLayout;)V
    .registers 5
    .param p1, "rootView"  # Landroidx/coordinatorlayout/widget/CoordinatorLayout;
    .param p2, "baseTextInputView"  # Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;
    .param p3, "button"  # Lcom/google/android/material/button/MaterialButton;
    .param p4, "coordinator"  # Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    .line 33
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 34
    iput-object p1, p0, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->rootView:Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    .line 35
    iput-object p2, p0, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->baseTextInputView:Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;

    .line 36
    iput-object p3, p0, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->button:Lcom/google/android/material/button/MaterialButton;

    .line 37
    iput-object p4, p0, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->coordinator:Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    .line 38
    return-void
.end method

.method public static bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;
    .registers 8
    .param p0, "rootView"  # Landroid/view/View;

    .line 68
    sget v0, Landroid/zero/studio/compose/preview/resources/R$id;->base_text_input_view:I

    .line 69
    .local v0, "id":I
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v1

    .line 70
    .local v1, "baseTextInputView":Landroid/view/View;
    if-eqz v1, :cond_23

    .line 73
    invoke-static {v1}, Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;->bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;

    move-result-object v2

    .line 75
    .local v2, "binding_baseTextInputView":Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;
    sget v0, Landroid/zero/studio/compose/preview/resources/R$id;->button:I

    .line 76
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v3

    check-cast v3, Lcom/google/android/material/button/MaterialButton;

    .line 77
    .local v3, "button":Lcom/google/android/material/button/MaterialButton;
    if-eqz v3, :cond_22

    .line 81
    move-object v4, p0

    check-cast v4, Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    .line 83
    .local v4, "coordinator":Landroidx/coordinatorlayout/widget/CoordinatorLayout;
    new-instance v5, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;

    move-object v6, p0

    check-cast v6, Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    invoke-direct {v5, v6, v2, v3, v4}, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;-><init>(Landroidx/coordinatorlayout/widget/CoordinatorLayout;Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;Lcom/google/android/material/button/MaterialButton;Landroidx/coordinatorlayout/widget/CoordinatorLayout;)V

    return-object v5

    .line 78
    .end local v4  # "coordinator":Landroidx/coordinatorlayout/widget/CoordinatorLayout;
    :cond_22
    goto :goto_24

    .line 71
    .end local v2  # "binding_baseTextInputView":Landroid/zero/studio/compose/preview/resources/databinding/BaseTextInputSingleBinding;
    .end local v3  # "button":Lcom/google/android/material/button/MaterialButton;
    :cond_23
    nop

    .line 86
    .end local v1  # "baseTextInputView":Landroid/view/View;
    :goto_24
    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v1

    invoke-virtual {v1, v0}, Landroid/content/res/Resources;->getResourceName(I)Ljava/lang/String;

    move-result-object v1

    .line 87
    .local v1, "missingId":Ljava/lang/String;
    new-instance v2, Ljava/lang/NullPointerException;

    const-string v3, "Missing required view with ID: "

    invoke-virtual {v3, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw v2
.end method

.method public static inflate(Landroid/view/LayoutInflater;)Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;
    .registers 3
    .param p0, "inflater"  # Landroid/view/LayoutInflater;

    .line 49
    const/4 v0, 0x0

    const/4 v1, 0x0

    invoke-static {p0, v0, v1}, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;

    move-result-object v0

    return-object v0
.end method

.method public static inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;
    .registers 5
    .param p0, "inflater"  # Landroid/view/LayoutInflater;
    .param p1, "parent"  # Landroid/view/ViewGroup;
    .param p2, "attachToParent"  # Z

    .line 55
    sget v0, Landroid/zero/studio/compose/preview/resources/R$layout;->base_single_button_and_text_input_single:I

    const/4 v1, 0x0

    invoke-virtual {p0, v0, p1, v1}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    .line 56
    .local v0, "root":Landroid/view/View;
    if-eqz p2, :cond_c

    .line 57
    invoke-virtual {p1, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    .line 59
    :cond_c
    invoke-static {v0}, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;

    move-result-object v1

    return-object v1
.end method


# virtual methods
.method public bridge synthetic getRoot()Landroid/view/View;
    .registers 2

    .line 18
    invoke-virtual {p0}, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->getRoot()Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    move-result-object v0

    return-object v0
.end method

.method public getRoot()Landroidx/coordinatorlayout/widget/CoordinatorLayout;
    .registers 2

    .line 43
    iget-object v0, p0, Landroid/zero/studio/compose/preview/resources/databinding/BaseSingleButtonAndTextInputSingleBinding;->rootView:Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    return-object v0
.end method
